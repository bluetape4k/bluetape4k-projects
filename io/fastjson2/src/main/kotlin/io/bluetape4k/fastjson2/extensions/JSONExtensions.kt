package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.reference
import java.io.InputStream

/**
 * 객체를 JSON 문자열로 직렬화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `JSON.toJSONString(this, *features)`를 호출합니다.
 * - 수신 객체가 null이면 null 문자열을 반환합니다.
 * - 직렬화 옵션은 [features]에 따라 적용됩니다.
 *
 * ```kotlin
 * val json = mapOf("id" to 1).toJsonString()
 * // json == "{\"id\":1}"
 * ```
 */
fun Any?.toJsonString(
    vararg features: JSONWriter.Feature,
): String? =
    JSON.toJSONString(this, *features)

/**
 * JSON 문자열을 [JSONObject]로 파싱합니다.
 *
 * ## 동작/계약
 * - `JSON.parseObject(this, *features)`를 호출합니다.
 * - 수신 문자열이 null이면 빈 객체가 아닌 fastjson2 기본 동작을 따릅니다.
 * - 파싱 오류는 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val obj = "{\"id\":1}".readAsJSONObject()
 * // obj["id"] == 1
 * ```
 */
fun String?.readAsJSONObject(
    vararg features: JSONReader.Feature,
): JSONObject = JSON.parseObject(this, *features)

/**
 * JSON 문자열을 지정 클래스 타입으로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `JSON.parseObject(this, clazz, *features)`를 호출합니다.
 * - 수신 문자열이 null이면 null을 반환할 수 있으며 fastjson2 동작을 따릅니다.
 * - 타입 불일치/파싱 실패는 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val map = "{\"id\":1}".readValueOrNull(Map::class.java)
 * // map == mapOf("id" to 1)
 * ```
 */
fun <T> String?.readValueOrNull(clazz: Class<T>, vararg features: JSONReader.Feature): T? =
    JSON.parseObject(this, clazz, *features)

/**
 * JSON 문자열을 reified 타입으로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `JSON.parseObject(this, reference<T>(), *features)`를 호출합니다.
 * - 제네릭 타입 매핑에 `reference<T>()`를 사용합니다.
 * - 파싱 실패 시 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val map: Map<String, Int>? = "{\"id\":1}".readValueOrNull()
 * // map == mapOf("id" to 1)
 * ```
 */
inline fun <reified T: Any> String?.readValueOrNull(vararg features: JSONReader.Feature): T? =
    JSON.parseObject(this, reference(), *features)

/**
 * JSON 배열 문자열을 reified 요소 타입 리스트로 변환합니다.
 *
 * ## 동작/계약
 * - 문자열이 null이면 빈 리스트를 반환합니다.
 * - null이 아니면 `JSON.parseArray`로 파싱합니다.
 * - 요소 매핑 실패는 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ids = "[1,2,3]".readValueAsList<Int>()
 * // ids == listOf(1, 2, 3)
 * ```
 */
inline fun <reified T: Any> String?.readValueAsList(vararg features: JSONReader.Feature): List<T> =
    this?.let { JSON.parseArray<T>(it, reference<T>().type, *features) }.orEmpty()

/**
 * 입력 스트림의 JSON 데이터를 reified 타입 객체로 역직렬화합니다.
 *
 * ## 동작/계약
 * - 수신 스트림이 null이면 null을 반환합니다.
 * - null이 아니면 `JSON.parseObject(stream, type, *features)`를 호출합니다.
 * - 스트림 소비 후 포인터는 EOF 위치로 이동합니다.
 *
 * ```kotlin
 * val input = """{"id":2}""".byteInputStream()
 * val map: Map<String, Int>? = input.readValueOrNull()
 * // map == mapOf("id" to 2)
 * ```
 */
inline fun <reified T: Any> InputStream?.readValueOrNull(vararg features: JSONReader.Feature): T? =
    this?.let { JSON.parseObject(it, reference<T>().type, *features) }

/**
 * 입력 스트림의 JSON 배열 데이터를 reified 요소 타입 리스트로 변환합니다.
 *
 * ## 동작/계약
 * - `JSON.parseArray(stream, *features)` 후 `readList<T>()`로 위임합니다.
 * - 스트림 전체를 소비합니다.
 * - 변환 실패 시 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val input = "[1,2,3]".byteInputStream()
 * val ids = input.readValueAsList<Int>()
 * // ids == listOf(1, 2, 3)
 * ```
 */
inline fun <reified T: Any> InputStream.readValueAsList(
    vararg features: JSONReader.Feature,
): List<T> =
    JSON.parseArray(this, *features).readList<T>(*features)

/**
 * 입력 스트림의 JSON 배열 데이터를 reified 요소 타입 배열로 변환합니다.
 *
 * ## 동작/계약
 * - `JSON.parseArray(stream, *features)` 후 `readArray<T>()`로 위임합니다.
 * - 스트림 전체를 소비하며 새 배열을 생성합니다.
 * - 타입 매핑 실패는 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val input = "[1,2,3]".byteInputStream()
 * val ids = input.readValueAsArray<Int>()
 * // ids.contentEquals(arrayOf(1, 2, 3)) == true
 * ```
 */
inline fun <reified T: Any> InputStream.readValueAsArray(
    vararg features: JSONReader.Feature,
): Array<T> =
    JSON.parseArray(this, *features).readArray<T>(*features)
