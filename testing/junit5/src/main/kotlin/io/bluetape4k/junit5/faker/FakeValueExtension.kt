package io.bluetape4k.junit5.faker

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import net.datafaker.Faker
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.util.stream.Stream

/**
 * [FakeValue]가 선언된 필드/파라미터에 DataFaker 값을 주입하는 JUnit5 확장입니다.
 *
 * ## 동작/계약
 * - `provider` 문자열을 `providerName.labelName`으로 분리해 리플렉션으로 값을 생성합니다.
 * - 컬렉션 대상은 `size`만큼 시퀀스를 소비해 변환하고, 단일 대상은 첫 값을 사용합니다.
 * - provider 경로나 타입이 맞지 않으면 리플렉션 예외가 발생할 수 있습니다.
 * - 필드 주입 실패는 로그 경고 후 다음 필드로 진행합니다.
 *
 * ```kotlin
 * @FakeValueTest
 * class Sample {
 *   @FakeValue(FakeValueProvider.Name.FullName) lateinit var name: String
 *   // name.isNotBlank() == true
 * }
 * ```
 */
class FakeValueExtension: TestInstancePostProcessor, ParameterResolver {

    companion object: KLogging() {

        private val faker = Fakers.faker

        private fun resolve(targetType: Class<*>, annotation: FakeValue): Any {
            log.trace { "resolve targetType=$targetType, annotation=$annotation" }

            return when {
                targetType.isAssignableFrom(List::class.java) || targetType.isAssignableFrom(Collection::class.java) ->
                    faker.getValues(annotation).toList()

                targetType.isAssignableFrom(Set::class.java)                                                         ->
                    faker.getValues(annotation).toSet()

                targetType.isAssignableFrom(Stream::class.java)                                                      ->
                    faker.getValues(annotation).toList().stream()

                targetType.isAssignableFrom(Sequence::class.java)                                                    ->
                    faker.getValues(annotation)

                else                                                                                                 ->
                    faker.getValues(annotation).first()
            }
        }

        private fun Faker.getValues(annotation: FakeValue): Sequence<Any> {
            log.trace { "get value. provider=${annotation.provider}" }

            val names = annotation.provider.split(".", limit = 2)
            val providerName = names[0]
            val labelName = names[1]
            log.trace { "providerName=$providerName, labelName=$labelName" }

            val providerMethod = javaClass.methods.find { it.name == providerName && it.parameterCount == 0 }!!
            val provider = providerMethod.invoke(this@getValues)

            val valueMethod = provider.javaClass.methods.find { it.name == labelName && it.parameterCount == 0 }!!

            return generateSequence { valueMethod.invoke(provider) }.take(annotation.size)
        }
    }

    /**
     * 파라미터가 [FakeValue]를 선언했는지 검사합니다.
     *
     * ## 동작/계약
     * - 어노테이션이 있으면 `true`, 없으면 `false`를 반환합니다.
     * - 추가 검증이나 타입 체크는 이 단계에서 수행하지 않습니다.
     */
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.getAnnotation(FakeValue::class.java) != null
    }

    /**
     * 테스트 파라미터에 주입할 fake 값을 생성합니다.
     *
     * ## 동작/계약
     * - 파라미터의 런타임 타입과 [FakeValue] 설정을 [resolve]에 전달합니다.
     * - 생성 실패 예외는 호출자(JUnit)로 전파됩니다.
     */
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return with(parameterContext.parameter) {
            resolve(type, getAnnotation(FakeValue::class.java))
        }
    }

    /**
     * 테스트 인스턴스의 `@FakeValue` 필드를 순회하며 값을 주입합니다.
     *
     * ## 동작/계약
     * - 선언 필드만 대상으로 하며 접근 제한자는 `isAccessible = true`로 해제합니다.
     * - 개별 필드 주입 실패는 경고 로그만 남기고 전체 처리를 중단하지 않습니다.
     */
    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        testInstance.javaClass.declaredFields.forEach { field ->
            val annotation = field.getAnnotation(FakeValue::class.java)
            annotation?.let {
                runCatching {
                    field.isAccessible = true
                    val fakeValue = resolve(field.type, annotation)
                    field[testInstance] = fakeValue
                }.onFailure {
                    log.warn(it) { "failed to inject fake value to field: $field" }
                }
            }
        }
    }
}
