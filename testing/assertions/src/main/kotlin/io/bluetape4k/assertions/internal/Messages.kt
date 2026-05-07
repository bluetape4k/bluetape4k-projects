package io.bluetape4k.assertions.internal

/**
 * assertion 실패 메시지를 생성하는 내부 유틸리티.
 *
 * 표준 포맷: `Expected <subject> to <verb> <expected>, but was <actual>.`
 */
internal object Messages {

    /**
     * 긍정 assertion 실패 메시지: "Expected <actual> to <verb> <expected>, but was not."
     */
    internal fun expectedToBe(verb: String, expected: Any?, actual: Any?): String =
        "Expected ${stringify(actual)} to $verb ${stringify(expected)}, but was not."

    /**
     * 부정 assertion 실패 메시지: "Expected <actual> not to <verb> <expected>, but was."
     */
    internal fun expectedNotToBe(verb: String, expected: Any?, actual: Any?): String =
        "Expected ${stringify(actual)} not to $verb ${stringify(expected)}, but was."

    /**
     * 단순 비교 실패 메시지: "Expected <expected>, but was <actual>."
     */
    internal fun comparison(expected: Any?, actual: Any?): String =
        "Expected ${stringify(expected)}, but was ${stringify(actual)}."

    /**
     * 값을 사람이 읽기 좋은 문자열로 변환한다.
     *
     * - null → "<null>"
     * - String → `"value"`
     * - CharSequence → `"value"`
     * - Throwable → ClassName: message
     * - Collection → [e1, e2, ...]
     * - Array → [e1, e2, ...]
     * - primitive arrays → [e1, e2, ...]
     * - 그 외 → value.toString()
     */
    internal fun stringify(value: Any?): String = when (value) {
        null -> "<null>"
        is String -> "\"$value\""
        is CharSequence -> "\"$value\""
        is Throwable -> "${value::class.simpleName}: ${value.message}"
        is Collection<*> -> value.joinToString(prefix = "[", postfix = "]") { stringify(it) }
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") {
            "${stringify(it.key)}=${stringify(it.value)}"
        }
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { stringify(it) }
        is IntArray -> value.joinToString(prefix = "[", postfix = "]")
        is LongArray -> value.joinToString(prefix = "[", postfix = "]")
        is DoubleArray -> value.joinToString(prefix = "[", postfix = "]")
        is FloatArray -> value.joinToString(prefix = "[", postfix = "]")
        is ByteArray -> value.joinToString(prefix = "[", postfix = "]")
        is ShortArray -> value.joinToString(prefix = "[", postfix = "]")
        is CharArray -> value.joinToString(prefix = "[", postfix = "]")
        is BooleanArray -> value.joinToString(prefix = "[", postfix = "]")
        else -> "<$value>"
    }
}
