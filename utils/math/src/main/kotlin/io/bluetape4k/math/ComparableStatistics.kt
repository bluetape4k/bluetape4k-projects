package io.bluetape4k.math


inline fun <T, K, C: Comparable<C>> Sequence<T>.minBy(keySelector: (T) -> K, valueSelector: (T) -> C): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.minOrNull() }

inline fun <T, K, C: Comparable<C>> Iterable<T>.minBy(keySelector: (T) -> K, valueSelector: (T) -> C): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.minOrNull() }

fun <K, C: Comparable<C>> Sequence<Pair<K, C>>.minBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.minOrNull() }

fun <K, C: Comparable<C>> Iterable<Pair<K, C>>.minBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.minOrNull() }


inline fun <T, K, C: Comparable<C>> Sequence<T>.maxBy(keySelector: (T) -> K, valueSelector: (T) -> C): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.maxOrNull() }

inline fun <T, K, C: Comparable<C>> Iterable<T>.maxBy(keySelector: (T) -> K, valueSelector: (T) -> C): Map<K, C?> =
    aggregateBy(keySelector, valueSelector) { it.maxOrNull() }

fun <K, C: Comparable<C>> Sequence<Pair<K, C>>.maxBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.maxOrNull() }

fun <K, C: Comparable<C>> Iterable<Pair<K, C>>.maxBy(): Map<K, C?> =
    aggregateBy({ it.first }, { it.second }) { it.maxOrNull() }


fun <C: Comparable<C>> Sequence<C>.range(): ClosedRange<C> = asIterable().range()

fun <C: Comparable<C>> Iterable<C>.range(): ClosedRange<C> =
    (minOrNull() ?: throw RuntimeException("At least one element must be present"))..
            (maxOrNull() ?: throw RuntimeException("At least one element must be present"))


inline fun <T, K, C: Comparable<C>> Sequence<T>.rangeBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, ClosedRange<C>> =
    aggregateBy(keySelector, valueSelector) { it.range() }

inline fun <T, K, C: Comparable<C>> Iterable<T>.rangeBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> C,
): Map<K, ClosedRange<C>> =
    aggregateBy(keySelector, valueSelector) { it.range() }
