package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * 요소들의 collect 해서, `Pair((n)-th, (n+1)th)` 를 요소로 emit 하는 Flow 를 빌드합니다. (zipWithNext 와 같다)
 *
 * ```
 * val pairs = flowOf(1,2,3,4,5).pairwise().toList()
 * // returns [Pair(1,2), Pair(2,3), Pair(3,4), Pair(4,5)]
 * ```
 *
 * @see [zipWithNext]
 */
fun <T> Flow<T>.pairwise(): Flow<Pair<T, T>> =
    pairwiseInternal { a, b -> a to b }

/**
 * 요소들의 collect 해서, `Pair((n)-th, (n+1)th)` 를 요소로 emit 하는 Flow 를 빌드합니다.
 *
 * ```
 * val pairs = flowOf(1,2,3,4,5).pairwise { a, b -> a to b }.toList()
 * // returns [Pair(1,2), Pair(2,3), Pair(3,4), Pair(4,5)]
 * ```
 * @see [zipWithNext]
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
