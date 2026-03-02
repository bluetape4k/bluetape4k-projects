package io.bluetape4k.math

/**
 * 키별 최소값 통계를 계산합니다.
 *
 * ## 동작/계약
 * - 같은 키의 값을 그룹화한 뒤 각 그룹의 최소값을 반환합니다.
 * - 입력이 비어 있으면 빈 맵을 반환합니다.
 *
 * ```kotlin
 * val result = sequenceOf("a" to 3, "a" to 1, "b" to 2).minBy()
 * // result["a"] == 1
 * // result["b"] == 2
 * ```
 */
inline fun <T: Any, K: Any, C: Comparable<C>> Sequence<T>.minBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.minOrNull() }

/**
 * 키별 최소값 통계를 계산합니다.
 *
 * ## 동작/계약
 * - [Sequence.minBy]의 `Iterable` 버전입니다.
 */
inline fun <T: Any, K: Any, C: Comparable<C>> Iterable<T>.minBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.minOrNull() }

/**
 * `(key, value)` 쌍 시퀀스에서 키별 최소값을 계산합니다.
 *
 * ## 동작/계약
 * - 키별 그룹 최소값을 반환하며, 비어 있으면 빈 맵입니다.
 */
fun <K: Any, C: Comparable<C>> Sequence<Pair<K, C>>.minBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.minOrNull() }

/**
 * `(key, value)` 쌍 iterable에서 키별 최소값을 계산합니다.
 */
fun <K: Any, C: Comparable<C>> Iterable<Pair<K, C>>.minBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.minOrNull() }


/**
 * 키별 최대값 통계를 계산합니다.
 *
 * ## 동작/계약
 * - 같은 키의 값을 그룹화한 뒤 각 그룹 최대값을 반환합니다.
 *
 * ```kotlin
 * val result = listOf("a" to 3, "a" to 1, "b" to 2).maxBy()
 * // result["a"] == 3
 * // result["b"] == 2
 * ```
 */
inline fun <T: Any, K: Any, C: Comparable<C>> Sequence<T>.maxBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.maxOrNull() }

/**
 * 키별 최대값 통계를 계산합니다.
 */
inline fun <T: Any, K: Any, C: Comparable<C>> Iterable<T>.maxBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.maxOrNull() }

/**
 * `(key, value)` 쌍 시퀀스에서 키별 최대값을 계산합니다.
 */
fun <K: Any, C: Comparable<C>> Sequence<Pair<K, C>>.maxBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.maxOrNull() }

/**
 * `(key, value)` 쌍 iterable에서 키별 최대값을 계산합니다.
 */
fun <K: Any, C: Comparable<C>> Iterable<Pair<K, C>>.maxBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.maxOrNull() }


/**
 * 시퀀스의 최소/최대 범위를 계산합니다.
 *
 * ## 동작/계약
 * - 요소가 하나 이상 있어야 하며, 비어 있으면 예외가 발생합니다.
 *
 * ```kotlin
 * val r = sequenceOf(1, 3, 2).range()
 * // r.start == 1
 * // r.endInclusive == 3
 * ```
 */
fun <C: Comparable<C>> Sequence<C>.range(): ClosedRange<C> = asIterable().range()

/**
 * Iterable의 최소/최대 범위를 계산합니다.
 *
 * ## 동작/계약
 * - 빈 입력이면 [RuntimeException]을 던집니다.
 */
fun <C: Comparable<C>> Iterable<C>.range(): ClosedRange<C> =
    (minOrNull() ?: throw RuntimeException("At least one element must be present"))..
            (maxOrNull() ?: throw RuntimeException("At least one element must be present"))

/**
 * 키별 값 범위(최소..최대)를 계산합니다.
 *
 * ## 동작/계약
 * - 그룹별로 [range]를 적용합니다.
 */
inline fun <T: Any, K: Any, C: Comparable<C>> Sequence<T>.rangeBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, ClosedRange<C>> =
    aggregateBy(keySelector, valueSelector) { it.range() }

/**
 * 키별 값 범위(최소..최대)를 계산합니다.
 */
inline fun <T: Any, K: Any, C: Comparable<C>> Iterable<T>.rangeBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, ClosedRange<C>> =
    aggregateBy(keySelector, valueSelector) { it.range() }
