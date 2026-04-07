package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.jackson3.JacksonSerializer
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import tools.jackson.databind.JsonNode

@PublishedApi
internal fun ResultRow.anyValueOrNull(expression: Expression<*>): Any? {
    @Suppress("UNCHECKED_CAST")
    return getOrNull(expression as Expression<Any?>)
}

@PublishedApi
internal fun requiredJacksonError(expression: Expression<*>, typeName: String): Nothing =
    error("Expression[$expression] is null or not convertible to $typeName.")

/**
 * [ResultRow]의 표현식 값을 Jackson3로 역직렬화해 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getJacksonOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 값이 `T` 타입이면 그대로 반환하고, 문자열/바이트 배열/`toString()` 경로로 역직렬화합니다.
 * - 컬럼 값이 `null`이거나 변환 실패 시 `IllegalStateException` 또는 Jackson 예외가 발생합니다.
 *
 * ```kotlin
 * val dto = row.getJackson<MyDto>(MyTable.payload)
 * // dto == MyDto(...)
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> ResultRow.getJackson(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T = getJacksonOrNull<T>(expression, serializer) ?: requiredJacksonError(expression, T::class.simpleName ?: "T")

/**
 * [ResultRow]의 표현식 값을 Jackson3로 역직렬화하고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `getOrNull(expression)` 결과가 `null`이면 즉시 `null`을 반환합니다.
 * - 값이 `T`이면 그대로 반환하고, 그 외에는 문자열/바이트/`toString()`으로 변환해 역직렬화합니다.
 * - JSON 변환 실패 시 Jackson 예외가 그대로 전파됩니다.
 *
 * ```kotlin
 * val dto = row.getJacksonOrNull<MyDto>(MyTable.payload)
 * // dto == null 또는 MyDto(...)
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> ResultRow.getJacksonOrNull(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is T      -> value
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

/**
 * [ResultRow]의 표현식 값을 [JsonNode]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getJsonNodeOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 값이 [JsonNode]이면 그대로 반환하며, 그 외 값은 `readTree`로 파싱합니다.
 * - JSON 파싱 실패 시 Jackson 예외가 발생합니다.
 *
 * ```kotlin
 * val node = row.getJsonNode(MyTable.payload)
 * // node.isObject == true
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 * @param serializer JSON 파싱에 사용할 serializer입니다.
 */
fun ResultRow.getJsonNode(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode = getJsonNodeOrNull(expression, serializer) ?: requiredJacksonError(expression, "JsonNode")

/**
 * [ResultRow]의 표현식 값을 [JsonNode]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 값이 이미 [JsonNode]면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값은 `serializer.mapper.readTree(...)`로 파싱합니다.
 * - JSON 형식이 아니면 Jackson 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val node = row.getJsonNodeOrNull(MyTable.payload)
 * // node == null 또는 JsonNode
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 * @param serializer JSON 파싱에 사용할 serializer입니다.
 */
fun ResultRow.getJsonNodeOrNull(
    expression: Expression<*>,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is JsonNode -> value
        is String   -> serializer.mapper.readTree(value)
        is ByteArray -> serializer.mapper.readTree(value)
        else        -> serializer.mapper.readTree(value.toString())
    }
}
