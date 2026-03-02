package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.reference

/**
 * [JSONObject] 전체를 reified 타입 객체로 변환합니다.
 *
 * ## 동작/계약
 * - 내부 `to(reference<T>(), *features)`를 호출해 전체 객체를 매핑합니다.
 * - 필드 매핑 실패 시 null이 반환되거나 예외가 발생할 수 있으며 fastjson2 동작을 따릅니다.
 * - 수신 `JSONObject`는 변경하지 않습니다.
 *
 * ```kotlin
 * val obj = """{"id":1,"name":"A"}""".readAsJSONObject()
 * val user: Map<String, Any>? = obj.readValueOrNull()
 * // user == mapOf("id" to 1, "name" to "A")
 * ```
 */
inline fun <reified T: Any> JSONObject.readValueOrNull(vararg features: JSONReader.Feature): T? =
    to(reference<T>(), *features)

/**
 * [JSONObject]의 특정 키 값을 reified 타입 객체로 변환합니다.
 *
 * ## 동작/계약
 * - [key]에 해당하는 값을 `getObject(key, reference<T>(), *features)`로 읽습니다.
 * - 키가 없거나 타입 매핑 실패 시 null을 반환할 수 있습니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val obj = """{"id":1}""".readAsJSONObject()
 * val id: Int? = obj.readValueOrNull("id")
 * // id == 1
 * ```
 */
inline fun <reified T: Any> JSONObject.readValueOrNull(
    key: String,
    vararg features: JSONReader.Feature,
): T? =
    getObject(key, reference<T>(), *features)
