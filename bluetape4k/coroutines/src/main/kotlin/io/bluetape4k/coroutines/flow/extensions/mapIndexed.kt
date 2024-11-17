package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow

/**
 * 원본 Flow의 요소을 받아서, 각 요소에 대해 인덱스와 함께 변환 함수를 적용한 Flow 를 생성합니다.
 *
 * ```
 * flowRangeOf(1, 4)
 *    .mapIndexed { index, value -> index to value }
 *    .assertResult(
 *        0 to 1,
 *        1 to 2,
 *        2 to 3,
 *        3 to 4
 *    )
 * ```
 *
 * @see [kotlinx.coroutines.flow.collectIndexed]
 */
inline fun <T, R> Flow<T>.mapIndexed(
    crossinline transform: suspend (index: Int, value: T) -> R,
): Flow<R> = flow {
    this@mapIndexed.collectIndexed { index, value ->
        emit(transform(index, value))
    }
}
