package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * 요소들의 collect 해서, `Pair((n)-th, (n+1)th)` 를 요소로 emit 하는 Flow 를 빌드합니다.
 *
 * ```
 * val zip = flowOf(1,2,3,4,5).zipWithNext().toList()
 * // returns [Pair(1,2), Pair(2,3), Pair(3,4), Pair(4,5)]
 * ```
 *
 * @see [pairwise]
 */
fun <T> Flow<T>.zipWithNext(): Flow<Pair<T, T>> = pairwise()

/**
 * 요소들의 collect 해서, `Pair((n)-th, (n+1)th)` 를 요소로 emit 하는 Flow 를 빌드합니다.
 *
 * ```
 * val pairs = flowOf(1,2,3,4,5).zipWithNext { a, b -> a to b }.toList()
 * // returns [Pair(1,2), Pair(2,3), Pair(3,4), Pair(4,5)]
 * ```
 *
 * @see [pairwise]
 */
fun <T, R> Flow<T>.zipWithNext(transform: suspend (a: T, b: T) -> R): Flow<R> = pairwise(transform)
