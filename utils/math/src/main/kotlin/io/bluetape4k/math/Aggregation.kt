package io.bluetape4k.math

inline fun <T, K, R> Sequence<T>.aggregateBy(
    keySelector: (T) -> K,
    aggregator: (elements: Iterable<T>) -> R,
): Map<K, R> =
    aggregateBy(keySelector, { it }, aggregator)

inline fun <T, K, R> Iterable<T>.aggregateBy(
    keySelector: (T) -> K,
    aggregator: (elements: Iterable<T>) -> R,
): Map<K, R> =
    aggregateBy(keySelector, { it }, aggregator)


/**
 * 컬렉션에 대해 집계를 수행합니다.
 *
 * @param keySelector 컬렉션 요소로부터 Group으로 묶을 기준이 되는 Key 값을 만들어내는 선택자
 * @param valueTransform 컬렉션 요소로부터 Group 해야 할 값을 선택하는 선택자
 * @param aggregator Grouping 된 값을 집계하는 함수
 * @return 컬렉션 요소들을 key - 요소 값의 집계 결과
 *
 * ```
 * data class Event(val eventTimestamp: Instant, val durationMs: Long)
 *
 * val sumOfDurationByHour = instances.aggregateBy(
 *      { it.eventTimestamp.trunk(TimeUnit.Hour) },
 *      { it.durationMs },
 *      { it.sum() }
 * )
 * ```
 */
inline fun <T, K, V, R> Sequence<T>.aggregateBy(
    keySelector: (T) -> K,
    valueTransform: (T) -> V,
    aggregator: (values: Iterable<V>) -> R,
): Map<K, R> {
    return groupBy(keySelector, valueTransform)
        .map { it.key to aggregator(it.value) }
        .toMap()
}

inline fun <T, K, V, R> Iterable<T>.aggregateBy(
    keySelector: (T) -> K,
    valueTransform: (T) -> V,
    aggregator: (values: Iterable<V>) -> R,
): Map<K, R> =
    asSequence().aggregateBy(keySelector, valueTransform, aggregator)
