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
 * [net.datafaker.Faker]를 이용해 [FakeValue]를 제공하는 테스트를 수행하도록 합니다.
 *
 * @see net.datafaker.Faker
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

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.getAnnotation(FakeValue::class.java) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return with(parameterContext.parameter) {
            resolve(type, getAnnotation(FakeValue::class.java))
        }
    }

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
