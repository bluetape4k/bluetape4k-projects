package io.bluetape4k.junit5.random

import io.bluetape4k.logging.KLogging
import io.github.benas.randombeans.api.EnhancedRandom
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * [RandomValue]가 선언된 필드와 파라미터에 random-beans 기반 값을 주입하는 JUnit5 확장입니다.
 *
 * ## 동작/계약
 * - 필드 주입은 테스트 인스턴스 생성 후 [postProcessTestInstance]에서 수행됩니다.
 * - 파라미터 주입은 [supportsParameter]가 `@RandomValue` 존재 여부로 판단합니다.
 * - 컬렉션/시퀀스/스트림 타입은 `size`와 `type`으로 여러 값을 생성하고, 단일 타입은 1개를 생성합니다.
 * - 별도 `require` 검증은 없으며 생성 실패 예외는 호출자(JUnit)로 전파됩니다.
 *
 * ```kotlin
 * @RandomizedTest
 * class SampleTest {
 *   @RandomValue lateinit var name: String
 *   // name.isNotBlank() == true
 * }
 * ```
 */
class RandomExtension: TestInstancePostProcessor, ParameterResolver {

    companion object: KLogging() {

        private val randomizer: EnhancedRandom by lazy(LazyThreadSafetyMode.NONE) { DefaultEnhancedRandom }

        private fun resolve(targetType: Class<*>, annotation: RandomValue): Any = when {
            targetType.isAssignableFrom(Set::class.java)      ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).asSequence().toSet()

            targetType.isAssignableFrom(List::class.java) || targetType.isAssignableFrom(Collection::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).toList()

            targetType.isAssignableFrom(Stream::class.java)   ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes)

            targetType.isAssignableFrom(Sequence::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).toList()

            targetType.isAssignableFrom(Iterator::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).iterator()

            targetType.isAssignableFrom(Iterable::class.java) ->
                randomizer.objects(annotation.type.java, annotation.size, *annotation.excludes).toList()

            targetType.isAssignableFrom(String::class.java)   ->
                randomizer.nextObject(String::class.java, *annotation.excludes).toString()
            // Fakers.randomString(2, 256)

            else                                              ->
                randomizer.nextObject(targetType, *annotation.excludes)
        }
    }

    /**
     * 테스트 인스턴스 필드 중 [RandomValue]가 선언된 항목에 랜덤 값을 주입합니다.
     */
    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        testInstance.javaClass.declaredFields.forEach { field ->
            field.getAnnotation(RandomValue::class.java)?.let { annotation ->
                field.isAccessible = true
                val randomObj = resolve(field.type, annotation)
                field[testInstance] = randomObj
            }
        }
    }

    /**
     * 파라미터가 [RandomValue]를 선언했는지 검사합니다.
     */
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.getAnnotation(RandomValue::class.java) != null
    }

    /**
     * 테스트 파라미터에 주입할 랜덤 값을 생성해 반환합니다.
     */
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return with(parameterContext.parameter) {
            resolve(type, getAnnotation(RandomValue::class.java))
        }
    }
}
