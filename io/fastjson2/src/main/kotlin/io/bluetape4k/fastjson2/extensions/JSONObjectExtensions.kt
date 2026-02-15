package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.reference

/**
 * [JSONObject] 전체를 reified 타입 [T]의 객체로 변환합니다.
 *
 * ```kotlin
 * val user = jsonObject.readValueOrNull<User>()
 * ```
 *
 * @param T 변환 대상 타입
 * @param features JSON 파싱 옵션
 * @return 변환된 객체. 실패 시 null 반환
 */
inline fun <reified T: Any> JSONObject.readValueOrNull(vararg features: JSONReader.Feature): T? =
    to(reference<T>(), *features)

/**
 * [JSONObject]에서 지정된 [key]에 해당하는 값을 reified 타입 [T]의 객체로 변환합니다.
 *
 * ```kotlin
 * val user = jsonObject.readValueOrNull<User>("key")
 * ```
 *
 * @param T 변환 대상 타입
 * @param key JSON 키
 * @param features JSON 파싱 옵션
 * @return 변환된 객체. 키가 존재하지 않거나 실패 시 null 반환
 */
inline fun <reified T: Any> JSONObject.readValueOrNull(
    key: String,
    vararg features: JSONReader.Feature,
): T? =
    getObject(key, reference<T>(), *features)
