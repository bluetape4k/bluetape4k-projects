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
 * [FieldSource]로 지정한 필드 값을 파라미터화 테스트 인자로 변환하는 provider입니다.
 *
 * ## 동작/계약
 * - 클래스 계층을 탐색해 필드를 찾고, static이 아니면 테스트 인스턴스에서 값을 읽습니다.
 * - 필드 값은 `Stream/Iterable/Array`만 허용하며 그 외 타입이면 [IllegalArgumentException]이 발생합니다.
 * - 필드 값이 `Arguments`면 그대로 사용하고, 배열이면 전개해 [Arguments.of]로 감쌉니다.
 * - 필드 미존재/null/인스턴스 부재 조건에서도 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * @FieldSource("cases")
 * @org.junit.jupiter.params.ParameterizedTest
 * fun sample(input: String, expected: Int) { /* ... */ }
 * // cases: listOf(argumentOf("A", 1), argumentOf("BB", 2))
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
