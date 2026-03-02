package io.bluetape4k.exposed.core.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.bluetape4k.fastjson2.FastjsonSerializer
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow

@PublishedApi
internal fun ResultRow.anyValueOrNull(expression: Expression<*>): Any? {
    @Suppress("UNCHECKED_CAST")
    return getOrNull(expression as Expression<Any?>)
}

@PublishedApi
internal fun requiredFastjsonError(expression: Expression<*>, typeName: String): Nothing =
    error("Expression[$expression] is null or not convertible to $typeName.")

/**
 * [ResultRow]의 표현식 값을 Fastjson2로 역직렬화해 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 값이 `T` 타입이면 그대로 반환하고, 그 외에는 문자열/바이트 배열/`toString()` 경로로 역직렬화합니다.
 * - 컬럼 값이 `null`이거나 변환 실패 시 `IllegalStateException` 또는 역직렬화 예외가 발생합니다.
 *
 * ```kotlin
 * val dto = row.getFastjson<MyDto>(MyTable.payload)
 * // dto == MyDto(...)
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> ResultRow.getFastjson(
    expression: Expression<*>,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T = getFastjsonOrNull<T>(expression, serializer) ?: requiredFastjsonError(expression, T::class.simpleName ?: "T")

/**
 * [ResultRow]의 표현식 값을 Fastjson2로 역직렬화하고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `getOrNull(expression)` 결과가 `null`이면 `null`을 반환합니다.
 * - 값이 `String`/`ByteArray`/`T` 타입이 아니면 `toString()` 결과를 JSON으로 해석합니다.
 * - JSON 파싱 실패 시 serializer 예외가 그대로 전파됩니다.
 *
 * ```kotlin
 * val dto = row.getFastjsonOrNull<MyDto>(MyTable.payload)
 * // dto == null 또는 MyDto(...)
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 * @param serializer JSON 직렬화/역직렬화에 사용할 serializer입니다.
 */
inline fun <reified T: Any> ResultRow.getFastjsonOrNull(
    expression: Expression<*>,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        is T -> value
        else -> serializer.deserializeFromString<T>(value.toString())
    }
}

/**
 * [ResultRow]의 표현식 값을 [JSONObject]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonObjectOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 값이 문자열/바이트 배열/기타 타입이면 `JSON.parseObject`로 파싱합니다.
 * - 객체 JSON 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val obj = row.getFastjsonObject(MyTable.payload)
 * // obj is JSONObject
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 */
fun ResultRow.getFastjsonObject(expression: Expression<*>): JSONObject =
    getFastjsonObjectOrNull(expression) ?: requiredFastjsonError(expression, "JSONObject")

/**
 * [ResultRow]의 표현식 값을 [JSONObject]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 값이 이미 [JSONObject]면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값은 `JSON.parseObject`로 파싱합니다.
 * - JSON 객체가 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val obj = row.getFastjsonObjectOrNull(MyTable.payload)
 * // obj == null 또는 JSONObject
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 */
fun ResultRow.getFastjsonObjectOrNull(expression: Expression<*>): JSONObject? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is JSONObject -> value
        is String -> JSON.parseObject(value)
        is ByteArray -> JSON.parseObject(value)
        else -> JSON.parseObject(value.toString())
    }
}

/**
 * [ResultRow]의 표현식 값을 [JSONArray]로 읽어 non-null 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getFastjsonArrayOrNull]을 호출하고 `null`이면 `error(...)`를 발생시킵니다.
 * - 값이 문자열/바이트 배열/기타 타입이면 `JSON.parseArray`로 파싱합니다.
 * - 배열 JSON 형식이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val arr = row.getFastjsonArray(MyTable.payload)
 * // arr is JSONArray
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 */
fun ResultRow.getFastjsonArray(expression: Expression<*>): JSONArray =
    getFastjsonArrayOrNull(expression) ?: requiredFastjsonError(expression, "JSONArray")

/**
 * [ResultRow]의 표현식 값을 [JSONArray]로 읽고, 원본 값이 `null`이면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 값이 이미 [JSONArray]면 그대로 반환합니다.
 * - 문자열/바이트 배열/기타 값은 `JSON.parseArray`로 파싱합니다.
 * - JSON 배열이 아니면 파싱 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val arr = row.getFastjsonArrayOrNull(MyTable.payload)
 * // arr == null 또는 JSONArray
 * ```
 *
 * @param expression 조회 대상 표현식입니다.
 */
fun ResultRow.getFastjsonArrayOrNull(expression: Expression<*>): JSONArray? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is JSONArray -> value
        is String -> JSON.parseArray(value)
        is ByteArray -> JSON.parseArray(value)
        else -> JSON.parseArray(value.toString())
    }
}
