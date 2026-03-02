package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * 인접한 두 요소를 `Pair`로 묶어 방출합니다.
 *
 * ## 동작/계약
 * - 길이 2의 슬라이딩 윈도우를 사용해 `(a, b)`를 생성합니다.
 * - 요소가 2개 미만이면 빈 Flow를 반환합니다.
 * - 윈도우 버퍼를 위해 요소 일부를 임시 저장합니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).pairwise().toList()
 * // result == [(1, 2), (2, 3)]
 * ```
 */
fun <T> Flow<T>.pairwise(): Flow<Pair<T, T>> =
    pairwiseInternal { a, b -> a to b }

/**
 * 인접한 두 요소에 변환 함수를 적용해 방출합니다.
 *
 * ## 동작/계약
 * - 요소가 2개 미만이면 transform이 호출되지 않습니다.
 * - 호출 순서는 원소 순서를 유지한 인접 쌍 순서입니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).pairwise { a, b -> a + b }.toList()
 * // result == [3, 5]
 * ```
 *
 * @param transform 인접한 두 값을 결과값으로 변환하는 함수입니다.
 */
fun <T, R> Flow<T>.pairwise(transform: suspend (a: T, b: T) -> R): Flow<R> =
    pairwiseInternal(transform)

private fun <T, R> Flow<T>.pairwiseInternal(transform: suspend (a: T, b: T) -> R): Flow<R> =
    sliding(2)
        .mapNotNull {
            when (it.size) {
                2    -> transform(it[0], it[1])
                else -> null
            }
        }
