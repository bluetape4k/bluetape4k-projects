package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.to

/**
 * [JSONArray] 전체를 reified 타입 객체로 변환합니다.
 *
 * ## 동작/계약
 * - 내부 `to<T>()`를 호출해 배열 전체를 하나의 객체로 매핑합니다.
 * - 매핑 실패 시 null이 반환될 수 있습니다.
 * - 수신 배열은 변경하지 않습니다.
 *
 * ```kotlin
 * val array = JSONArray.of(1, 2, 3)
 * val list: List<Int>? = array.readValueOrNull()
 * // list == listOf(1, 2, 3)
 * ```
 */
inline fun <reified T: Any> JSONArray.readValueOrNull(): T? = to<T>()

/**
 * [JSONArray]의 특정 인덱스 요소를 reified 타입으로 변환합니다.
 *
 * ## 동작/계약
 * - 내부 `to<T>(index, *features)`를 호출합니다.
 * - 인덱스 범위를 벗어나면 null 반환 또는 예외 발생은 fastjson2 동작을 따릅니다.
 * - 타입 변환 실패 시 null이 반환될 수 있습니다.
 *
 * ```kotlin
 * val array = JSONArray.of(10, 20, 30)
 * val first: Int? = array.readValueOrNull(0)
 * // first == 10
 * ```
 */
inline fun <reified T: Any> JSONArray.readValueOrNull(
    index: Int,
    vararg features: JSONReader.Feature,
): T? = to<T>(index, *features)

/**
 * [JSONArray]를 reified 요소 타입의 리스트로 변환합니다.
 *
 * ## 동작/계약
 * - `toList(T::class.java, *features)`를 호출합니다.
 * - 배열 길이에 비례하는 새 리스트가 할당됩니다.
 * - 변환 실패 항목 처리 방식은 fastjson2 구현을 따릅니다.
 *
 * ```kotlin
 * val ids = JSONArray.of(1, 2, 3).readList<Int>()
 * // ids == listOf(1, 2, 3)
 * ```
 */
inline fun <reified T> JSONArray.readList(vararg features: JSONReader.Feature): List<T> =
    toList(T::class.java, *features)

/**
 * [JSONArray]를 reified 요소 타입의 배열로 변환합니다.
 *
 * ## 동작/계약
 * - `toArray(T::class.java, *features)`를 호출합니다.
 * - 배열 길이에 비례해 새 배열이 할당됩니다.
 * - 타입 매핑 실패는 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val ids = JSONArray.of(1, 2, 3).readArray<Int>()
 * // ids.contentEquals(arrayOf(1, 2, 3)) == true
 * ```
 */
inline fun <reified T> JSONArray.readArray(vararg features: JSONReader.Feature): Array<T> =
    toArray(T::class.java, *features)
