package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.reference
import java.io.InputStream

/**
 * 객체를 JSON 문자열로 변환합니다.
 *
 * @param features JSON 직렬화 옵션
 * @return JSON 문자열. null 객체인 경우 null 반환
 */
fun Any?.toJsonString(
    vararg features: JSONWriter.Feature,
): String? =
    JSON.toJSONString(this, *features)

/**
 * JSON 문자열을 [JSONObject]로 파싱합니다.
 *
 * @param features JSON 파싱 옵션
 * @return 파싱된 [JSONObject]
 */
fun String?.readAsJSONObject(
    vararg features: JSONReader.Feature,
): JSONObject = JSON.parseObject(this, *features)

/**
 * JSON 문자열을 지정된 클래스 [clazz] 타입의 객체로 역직렬화합니다.
 *
 * @param T 역직렬화 대상 타입
 * @param clazz 역직렬화할 대상 클래스
 * @param features JSON 파싱 옵션
 * @return 역직렬화된 객체. 실패 시 null 반환
 */
fun <T> String?.readValueOrNull(clazz: Class<T>, vararg features: JSONReader.Feature): T? =
    JSON.parseObject(this, clazz, *features)

/**
 * JSON 문자열을 reified 타입 [T]의 객체로 역직렬화합니다.
 *
 * ```kotlin
 * val user = jsonString.readValueOrNull<User>()
 * ```
 *
 * @param T 역직렬화 대상 타입
 * @param features JSON 파싱 옵션
 * @return 역직렬화된 객체. 실패 시 null 반환
 */
inline fun <reified T: Any> String?.readValueOrNull(vararg features: JSONReader.Feature): T? =
    JSON.parseObject(this, reference(), *features)

/**
 * JSON 배열 문자열을 [List]로 역직렬화합니다.
 *
 * ```kotlin
 * val users = jsonArrayString.readValueAsList<User>()
 * ```
 *
 * @param T 리스트 요소 타입
 * @param features JSON 파싱 옵션
 * @return 역직렬화된 리스트. null 문자열인 경우 빈 리스트 반환
 */
inline fun <reified T: Any> String?.readValueAsList(vararg features: JSONReader.Feature): List<T> =
    this?.let { JSON.parseArray<T>(it, reference<T>().type, *features) }.orEmpty()

/**
 * [InputStream]에서 JSON 데이터를 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
 *
 * @param T 역직렬화 대상 타입
 * @param features JSON 파싱 옵션
 * @return 역직렬화된 객체. null 스트림인 경우 null 반환
 */
inline fun <reified T: Any> InputStream?.readValueOrNull(vararg features: JSONReader.Feature): T? =
    this?.let { JSON.parseObject(it, reference<T>().type, *features) }

/**
 * [InputStream]에서 JSON 배열 데이터를 읽어 [List]로 역직렬화합니다.
 *
 * @param T 리스트 요소 타입
 * @param features JSON 파싱 옵션
 * @return 역직렬화된 리스트
 */
inline fun <reified T: Any> InputStream.readValueAsList(
    vararg features: JSONReader.Feature,
): List<T> =
    JSON.parseArray(this, *features).readList<T>(*features)

/**
 * [InputStream]에서 JSON 배열 데이터를 읽어 [Array]로 역직렬화합니다.
 *
 * @param T 배열 요소 타입
 * @param features JSON 파싱 옵션
 * @return 역직렬화된 배열
 */
inline fun <reified T: Any> InputStream.readValueAsArray(
    vararg features: JSONReader.Feature,
): Array<T> =
    JSON.parseArray(this, *features).readArray<T>(*features)
