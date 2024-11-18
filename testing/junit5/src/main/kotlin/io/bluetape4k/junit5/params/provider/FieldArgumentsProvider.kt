package io.bluetape4k.junit5.params.provider

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.AnnotationConsumer
import java.lang.reflect.Field
import java.util.stream.Stream

/**
 * Field variable로 Arguments 를 제공하는 Provider 입니다.
 *
 * ```
 * val arguments: List<Arguments> = listOf(
 *         Arguments.of(null, true),
 *         Arguments.of("", true),
 *         Arguments.of("  ", true),
 *         Arguments.of("not blank", false)
 *     )
 *
 * @ParameterizedTest
 * @FieldSource("arguments")
 * fun `isBlank should return true for null or blank string variable`(input:String, expected:Boolean) {
 *     Strings.isBlank(input) shouldBeEqualTo expected
 * }
 * ```
 */
class FieldArgumentsProvider: ArgumentsProvider, AnnotationConsumer<FieldSource> {

    private lateinit var variableName: String

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
        return context.testClass
            .map { getField(it) }
            .map { getValue(it, context.testInstance.get()) }
            .orElseThrow { IllegalArgumentException("Fail to load test arguments") }
    }

    override fun accept(fieldSource: FieldSource) {
        variableName = fieldSource.value
    }

    private fun getField(clazz: Class<*>): Field? {
        return runCatching { clazz.getDeclaredField(variableName) }.getOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getValue(field: Field?, instance: Any): Stream<Arguments>? {
        return runCatching {
            field?.isAccessible = true
            val arguments = field?.get(instance) as? List<Arguments>
            arguments?.stream()
        }.getOrNull()
    }
}
