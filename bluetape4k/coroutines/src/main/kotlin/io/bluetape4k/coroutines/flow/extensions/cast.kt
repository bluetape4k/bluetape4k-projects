package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * [Flow]의 요소를 [R] 수형으로 casting 합니다.
 *
 * ```
 * val flowOf<Any?>(1,2,3).cast<Int>().toList()  // 1,2,3
 * ```
 *
 * ```
 * flowOf(1, 2, 3).cast<String>().collect()     // throw ClassCastException
 * ```
 *
 * @param R casting 할 수형
 * @return [R]로 casting 된 요소를 가진 [Flow]
 */
inline fun <reified R> Flow<*>.cast(): Flow<R> = map { it as R }

/**
 * [Flow]의 요소를 [R] 수형으로 casting 합니다. cast 실패 시에는 제외합니다.
 *
 * ```
 * flowOf(1,2,null, 3).castNotNull<Int>().toList()  // 1,2,3
 * ```
 *
 * @param R casting 할 수형
 * @return casting 에 성공한 요소만을 제공하는 [Flow]
 */
inline fun <reified R: Any> Flow<*>.castNotNull(): Flow<R> = mapNotNull { it as? R }

/**
 * 수형을 nullable 로 casting 합니다.
 *
 * ```
 * val flow: Flow<Int> = flowOf<Int>(1, 2, 3)
 * val flow2: Flow<Int?> = flow.castNullable()
 * ```
 *
 * @param T 원본 수형
 * @return Nullable 수형으로 변환된 [Flow]
 */
fun <T: Any> Flow<T>.castNullable(): Flow<T?> = this
