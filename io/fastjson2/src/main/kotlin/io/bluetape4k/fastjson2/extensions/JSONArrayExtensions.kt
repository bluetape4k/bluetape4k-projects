package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.to

/**
 * [JSONArray] 전체를 reified 타입 [T]의 객체로 변환합니다.
 *
 * @param T 변환 대상 타입
 * @return 변환된 객체. 실패 시 null 반환
 */
inline fun <reified T: Any> JSONArray.readValueOrNull(): T? = to<T>()

/**
 * [JSONArray]의 특정 인덱스 요소를 reified 타입 [T]의 객체로 변환합니다.
 *
 * ```kotlin
 * val user = jsonArray.readValueOrNull<User>(0)
 * ```
 *
 * @param T 변환 대상 타입
 * @param index 변환할 요소의 인덱스
 * @param features JSON 파싱 옵션
 * @return 변환된 객체. 실패 시 null 반환
 */
inline fun <reified T: Any> JSONArray.readValueOrNull(
    index: Int,
    vararg features: JSONReader.Feature,
): T? = to<T>(index, *features)

/**
 * [JSONArray]의 모든 요소를 [List]로 변환합니다.
 *
 * ```kotlin
 * val users = jsonArray.readList<User>()
 * ```
 *
 * @param T 리스트 요소 타입
 * @param features JSON 파싱 옵션
 * @return 변환된 리스트
 */
inline fun <reified T> JSONArray.readList(vararg features: JSONReader.Feature): List<T> =
    toList(T::class.java, *features)

/**
 * [JSONArray]의 모든 요소를 [Array]로 변환합니다.
 *
 * ```kotlin
 * val users = jsonArray.readArray<User>()
 * ```
 *
 * @param T 배열 요소 타입
 * @param features JSON 파싱 옵션
 * @return 변환된 배열
 */
inline fun <reified T> JSONArray.readArray(vararg features: JSONReader.Feature): Array<T> =
    toArray(T::class.java, *features)
