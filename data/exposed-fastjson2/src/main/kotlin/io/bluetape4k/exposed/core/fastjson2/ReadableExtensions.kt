package io.bluetape4k.exposed.core.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.r2dbc.spi.Readable

@PublishedApi
internal fun requiredFastjsonReadableError(column: Any, typeName: String): Nothing =
    error("Column[$column] is null or not convertible to $typeName.")

/**
 * 인덱스 컬럼 값을 Fastjson2로 역직렬화해 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 입력 값 타입이 `String`/`ByteArray`/`T`가 아니면 `toString()` 결과를 JSON으로 해석합니다.
 * - 컬럼 값이 `null`이거나 변환에 실패하면 `IllegalStateException` 또는 역직렬화 예외가 발생합니다.
 *
 * ```kotlin
 * val dto = row.getFastjson<MyDto>(0)
 * // dto == MyDto(...)
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getFastjson(
    index: Int,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T = getFastjsonOrNull<T>(index, serializer) ?: requiredFastjsonReadableError(index, T::class.simpleName ?: "T")

/**
 * 이름 컬럼 값을 Fastjson2로 역직렬화해 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 입력 값 타입 처리 순서는 `T` 직접 반환, `String`, `ByteArray`, 기타 `toString()` 순서입니다.
 * - 컬럼 값이 `null`이거나 변환 불가하면 `IllegalStateException` 또는 역직렬화 예외가 발생합니다.
 *
 * ```kotlin
 * val dto = row.getFastjson<MyDto>("payload")
 * // dto == MyDto(...)
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getFastjson(
    name: String,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T = getFastjsonOrNull<T>(name, serializer) ?: requiredFastjsonReadableError(name, T::class.simpleName ?: "T")

/**
 * 인덱스 컬럼 값을 Fastjson2로 역직렬화하고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `Readable.get(index)` 결과가 `null`이면 즉시 `null`을 반환합니다.
 * - 값이 이미 `T` 타입이면 역직렬화 없이 그대로 반환합니다.
 * - JSON 파싱 실패 시 serializer 예외가 그대로 전파됩니다.
 *
 * ```kotlin
 * val dto = row.getFastjsonOrNull<MyDto>(0)
 * // dto == null 또는 MyDto(...)
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getFastjsonOrNull(
    index: Int,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T? {
    val value = get(index) ?: return null
    return when (value) {
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        is T      -> value
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

/**
 * 이름 컬럼 값을 Fastjson2로 역직렬화하고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `Readable.get(name)` 결과가 `null`이면 즉시 `null`을 반환합니다.
 * - 값이 `String`/`ByteArray`/`T`가 아닌 경우 `toString()` 값을 JSON으로 간주합니다.
 * - JSON 파싱 실패 시 serializer 예외가 그대로 전파됩니다.
 *
 * ```kotlin
 * val dto = row.getFastjsonOrNull<MyDto>("payload")
 * // dto == null 또는 MyDto(...)
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> Readable.getFastjsonOrNull(
    name: String,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T? {
    val value = get(name) ?: return null
    return when (value) {
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        is T      -> value
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

/**
 * 인덱스 컬럼 값을 [JSONObject]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonObjectOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 문자열/바이트 배열/기타 객체의 `toString()`을 `JSON.parseObject`로 파싱합니다.
 * - 파싱 실패 시 Fastjson2 예외가 전파됩니다.
 *
 * ```kotlin
 * val obj = row.getFastjsonObject(0)
 * // obj is JSONObject
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 */
fun Readable.getFastjsonObject(index: Int): JSONObject =
    getFastjsonObjectOrNull(index) ?: requiredFastjsonReadableError(index, "JSONObject")

/**
 * 이름 컬럼 값을 [JSONObject]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonObjectOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 문자열/바이트 배열/기타 객체를 JSONObject로 파싱합니다.
 * - 파싱 실패 시 Fastjson2 예외가 전파됩니다.
 *
 * ```kotlin
 * val obj = row.getFastjsonObject("payload")
 * // obj is JSONObject
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 */
fun Readable.getFastjsonObject(name: String): JSONObject =
    getFastjsonObjectOrNull(name) ?: requiredFastjsonReadableError(name, "JSONObject")

/**
 * 인덱스 컬럼 값을 [JSONObject]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 컬럼 값이 `JSONObject`면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값은 `JSON.parseObject`로 파싱합니다.
 * - JSON 객체 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val obj = row.getFastjsonObjectOrNull(0)
 * // obj == null 또는 JSONObject
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 */
fun Readable.getFastjsonObjectOrNull(index: Int): JSONObject? {
    val value = get(index) ?: return null
    return when (value) {
        is JSONObject -> value
        is String    -> JSON.parseObject(value)
        is ByteArray -> JSON.parseObject(value)
        else         -> JSON.parseObject(value.toString())
    }
}

/**
 * 이름 컬럼 값을 [JSONObject]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 컬럼 값이 `JSONObject`면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값은 `JSON.parseObject`로 파싱합니다.
 * - JSON 객체 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val obj = row.getFastjsonObjectOrNull("payload")
 * // obj == null 또는 JSONObject
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 */
fun Readable.getFastjsonObjectOrNull(name: String): JSONObject? {
    val value = get(name) ?: return null
    return when (value) {
        is JSONObject -> value
        is String    -> JSON.parseObject(value)
        is ByteArray -> JSON.parseObject(value)
        else         -> JSON.parseObject(value.toString())
    }
}

/**
 * 인덱스 컬럼 값을 [JSONArray]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonArrayOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 문자열/바이트 배열/기타 객체를 `JSON.parseArray`로 파싱합니다.
 * - JSON 배열 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val arr = row.getFastjsonArray(0)
 * // arr is JSONArray
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 */
fun Readable.getFastjsonArray(index: Int): JSONArray =
    getFastjsonArrayOrNull(index) ?: requiredFastjsonReadableError(index, "JSONArray")

/**
 * 이름 컬럼 값을 [JSONArray]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonArrayOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 문자열/바이트 배열/기타 객체를 `JSON.parseArray`로 파싱합니다.
 * - JSON 배열 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val arr = row.getFastjsonArray("items")
 * // arr is JSONArray
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 */
fun Readable.getFastjsonArray(name: String): JSONArray =
    getFastjsonArrayOrNull(name) ?: requiredFastjsonReadableError(name, "JSONArray")

/**
 * 인덱스 컬럼 값을 [JSONArray]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 컬럼 값이 `JSONArray`면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값은 `JSON.parseArray`로 파싱합니다.
 * - JSON 배열 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val arr = row.getFastjsonArrayOrNull(0)
 * // arr == null 또는 JSONArray
 * ```
 *
 * @param index 조회할 컬럼 인덱스입니다.
 */
fun Readable.getFastjsonArrayOrNull(index: Int): JSONArray? {
    val value = get(index) ?: return null
    return when (value) {
        is JSONArray -> value
        is String -> JSON.parseArray(value)
        is ByteArray -> JSON.parseArray(value)
        else      -> JSON.parseArray(value.toString())
    }
}

/**
 * 이름 컬럼 값을 [JSONArray]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 컬럼 값이 `JSONArray`면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값은 `JSON.parseArray`로 파싱합니다.
 * - JSON 배열 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val arr = row.getFastjsonArrayOrNull("items")
 * // arr == null 또는 JSONArray
 * ```
 *
 * @param name 조회할 컬럼 이름입니다.
 */
fun Readable.getFastjsonArrayOrNull(name: String): JSONArray? {
    val value = get(name) ?: return null
    return when (value) {
        is JSONArray -> value
        is String -> JSON.parseArray(value)
        is ByteArray -> JSON.parseArray(value)
        else      -> JSON.parseArray(value.toString())
    }
}
