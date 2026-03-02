package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * 인접한 두 요소를 `(이전, 현재)` 쌍으로 묶어 방출합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [pairwise]에 위임합니다.
 * - 요소가 2개 미만이면 아무 것도 방출하지 않습니다.
 * - 결과 개수는 `max(원소수 - 1, 0)`입니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).zipWithNext().toList()
 * // result == [(1, 2), (2, 3)]
 * ```
 */
fun <T> Flow<T>.zipWithNext(): Flow<Pair<T, T>> = pairwise()

/**
 * 인접한 두 요소에 변환 함수를 적용해 방출합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [pairwise] 변환 버전에 위임합니다.
 * - 요소가 2개 미만이면 transform은 호출되지 않습니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).zipWithNext { a, b -> a + b }.toList()
 * // result == [3, 5]
 * ```
 *
 * @param transform 인접 요소 쌍을 결과값으로 변환하는 함수입니다.
 */
fun <T, R> Flow<T>.zipWithNext(transform: suspend (a: T, b: T) -> R): Flow<R> = pairwise(transform)
