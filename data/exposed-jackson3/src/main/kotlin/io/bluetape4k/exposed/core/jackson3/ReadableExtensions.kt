package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.jackson3.JacksonSerializer
import io.r2dbc.spi.Readable
import tools.jackson.databind.JsonNode

@PublishedApi
internal fun requiredJacksonReadableError(column: Any, typeName: String): Nothing =
    error("Column[$column] is null or not convertible to $typeName.")

/**
 * 인덱스 컬럼 값을 Jackson3로 역직렬화해 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getJacksonOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 값 타입이 `T`이면 그대로 반환하고, `String`/`ByteArray`/기타(`toString()`) 순서로 역직렬화합니다.
 * - 컬럼 값이 `null`이거나 변환 실패 시 `IllegalStateException` 또는 Jackson 예외가 발생합니다.
 *
 * ```kotlin
 * val dto = row.getJackson<MyDto>(0)
 * // dto == MyDto(...)
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getJackson(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T = getJacksonOrNull<T>(index, serializer) ?: requiredJacksonReadableError(index, T::class.simpleName ?: "T")

/**
 * 이름 컬럼 값을 Jackson3로 역직렬화해 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getJacksonOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 값 타입이 `T`이면 그대로 반환하고, 나머지는 문자열/바이트/`toString()` 경로로 역직렬화합니다.
 * - 컬럼 값이 `null`이거나 변환 실패 시 `IllegalStateException` 또는 Jackson 예외가 발생합니다.
 *
 * ```kotlin
 * val dto = row.getJackson<MyDto>("payload")
 * // dto == MyDto(...)
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getJackson(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T = getJacksonOrNull<T>(name, serializer) ?: requiredJacksonReadableError(name, T::class.simpleName ?: "T")

/**
 * 인덱스 컬럼 값을 Jackson3로 역직렬화하고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `Readable.get(index)`가 `null`이면 즉시 `null`을 반환합니다.
 * - 값이 이미 `T` 타입이면 역직렬화 없이 그대로 반환합니다.
 * - JSON 변환 실패 시 Jackson 예외가 그대로 전파됩니다.
 *
 * ```kotlin
 * val dto = row.getJacksonOrNull<MyDto>(0)
 * // dto == null 또는 MyDto(...)
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getJacksonOrNull(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T? {
    val value = get(index) ?: return null
    return when (value) {
        is T      -> value
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

/**
 * 이름 컬럼 값을 Jackson3로 역직렬화하고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `Readable.get(name)`가 `null`이면 즉시 `null`을 반환합니다.
 * - 값이 `String`/`ByteArray`/`T`가 아니면 `toString()` 결과를 JSON으로 해석합니다.
 * - JSON 변환 실패 시 Jackson 예외가 그대로 전파됩니다.
 *
 * ```kotlin
 * val dto = row.getJacksonOrNull<MyDto>("payload")
 * // dto == null 또는 MyDto(...)
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getJacksonOrNull(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): T? {
    val value = get(name) ?: return null
    return when (value) {
        is T      -> value
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

/**
 * 인덱스 컬럼 값을 [JsonNode]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getJsonNodeOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 문자열/바이트 배열/기타 값을 `serializer.mapper.readTree(...)`로 파싱합니다.
 * - 파싱 실패 시 Jackson 예외가 발생합니다.
 *
 * ```kotlin
 * val node = row.getJsonNode(0)
 * // node.isObject == true
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 * @param serializer JSON 파싱에 사용할 serializer입니다.
 */
fun Readable.getJsonNode(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode = getJsonNodeOrNull(index, serializer) ?: requiredJacksonReadableError(index, "JsonNode")

/**
 * 이름 컬럼 값을 [JsonNode]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getJsonNodeOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 문자열/바이트 배열/기타 값을 `serializer.mapper.readTree(...)`로 파싱합니다.
 * - 파싱 실패 시 Jackson 예외가 발생합니다.
 *
 * ```kotlin
 * val node = row.getJsonNode("payload")
 * // node.isObject == true
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 * @param serializer JSON 파싱에 사용할 serializer입니다.
 */
fun Readable.getJsonNode(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode = getJsonNodeOrNull(name, serializer) ?: requiredJacksonReadableError(name, "JsonNode")

/**
 * 인덱스 컬럼 값을 [JsonNode]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 값이 이미 [JsonNode]면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값을 `readTree`로 파싱합니다.
 * - JSON 형식이 아니면 Jackson 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val node = row.getJsonNodeOrNull(0)
 * // node == null 또는 JsonNode
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 * @param serializer JSON 파싱에 사용할 serializer입니다.
 */
fun Readable.getJsonNodeOrNull(
    index: Int,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode? {
    val value = get(index) ?: return null
    return when (value) {
        is JsonNode -> value
        is String   -> serializer.mapper.readTree(value)
        is ByteArray -> serializer.mapper.readTree(value)
        else        -> serializer.mapper.readTree(value.toString())
    }
}

/**
 * 이름 컬럼 값을 [JsonNode]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 값이 이미 [JsonNode]면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값을 `readTree`로 파싱합니다.
 * - JSON 형식이 아니면 Jackson 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val node = row.getJsonNodeOrNull("payload")
 * // node == null 또는 JsonNode
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 * @param serializer JSON 파싱에 사용할 serializer입니다.
 */
fun Readable.getJsonNodeOrNull(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): JsonNode? {
    val value = get(name) ?: return null
    return when (value) {
        is JsonNode -> value
        is String   -> serializer.mapper.readTree(value)
        is ByteArray -> serializer.mapper.readTree(value)
        else        -> serializer.mapper.readTree(value.toString())
    }
}
