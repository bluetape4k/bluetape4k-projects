package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * Flow 를 [size] 만큼씩 조각내어 `Flow<List<T>>` 로 변한합니다.
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val chunked = flow.chunked(3)   // [1,2,3], [4,5]
 * ```
 *
 * ```
 * val flow = flowRangeOf(1, 10)   // 1,2,3,4,5,6,7,8,9,10
 * val chunked = flow.chunked(3)   // [1,2,3], [4,5,6], [7,8,9], [10]
 * ```
 *
 * @param size chunk 크기. 0보다 커야 합니다.
 */
fun <T> Flow<T>.chunked(size: Int, partialWindow: Boolean = true): Flow<List<T>> =
    windowed(size, size, partialWindow)
