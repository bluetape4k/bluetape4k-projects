package io.bluetape4k.junit5.params.provider

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.AnnotationConsumer
import org.junit.jupiter.params.support.ParameterDeclarations
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.stream.Stream
import kotlin.streams.asStream

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

    override fun provideArguments(paramDefs: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        check(::variableName.isInitialized) { "FieldSource.value must be provided." }

        val testClass = context.testClass.orElseThrow {
            IllegalArgumentException("Fail to load test class from ExtensionContext.")
        }
        val field = findField(testClass, variableName) ?: throw IllegalArgumentException(
            "Cannot find field '$variableName' in ${testClass.name}."
        )
        val fieldValue = getFieldValue(field, context)

        return toArgumentsStream(fieldValue)
    }

    override fun accept(fieldSource: FieldSource) {
        variableName = fieldSource.value
    }

    private fun findField(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            runCatching { current.getDeclaredField(name) }.getOrNull()?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun getFieldValue(field: Field, context: ExtensionContext): Any {
        field.isAccessible = true
        val receiver = when {
            Modifier.isStatic(field.modifiers) -> null
            else                               -> context.testInstance.orElseThrow {
                IllegalArgumentException("Field '$variableName' requires a test instance.")
            }
        }
        return field.get(receiver)
            ?: throw IllegalArgumentException("Field '$variableName' resolved to null.")
    }

    private fun toArgumentsStream(value: Any): Stream<out Arguments> = when (value) {
        is Stream<*>   -> value.map { it.toArguments() }
        is Iterable<*> -> value.asSequence().map { it.toArguments() }.asStream()
        is Array<*>    -> value.asSequence().map { it.toArguments() }.asStream()
        else           -> throw IllegalArgumentException(
            "Field '$variableName' must be Stream/Iterable/Array, but was ${value::class.java.name}."
        )
    }

    private fun Any?.toArguments(): Arguments = when (this) {
        is Arguments -> this
        is Array<*>  -> Arguments.of(*this)
        else         -> Arguments.of(this)
    }
}
