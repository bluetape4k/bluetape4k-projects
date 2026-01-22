package io.bluetape4k.math

/**
 * Sequence에 대해 최종 집계를 손쉽게 하기 위해 `groupingBy` 함수를 이용하여 [Grouping] 을 사용하여 집계합니다.
 * @see [aggregateBy] 는 Group을 먼저 만들고, 집계함수를 수행하는데 이는 [Grouping]을 사용하는 것보다 비효율적이다 (Iterable vs Sequence 와 같다)
 *
 * @param keySelector 집계 기준이 되는 Key 선택 함수
 * @param aggregator 집계 함수
 * @return Key 기준의 집계 값의 Map
 */
inline fun <T: Any, K: Any, R: Any> Sequence<T>.groupingAggregate(
    crossinline keySelector: (T) -> K,
    @BuilderInference aggregator: (key: K, accumulator: R?, eleemnt: T, first: Boolean) -> R,
): Map<K, R> {
    return groupingBy(keySelector).aggregate(aggregator)
}

/**
 * Sequence에 대해 최종 집계를 손쉽게 하기 위해 `groupingBy` 함수를 이용하여 [Grouping] 을 사용하여 집계합니다.
 * @see [aggregateBy] 는 Group을 먼저 만들고, 집계함수를 수행하는데 이는 [Grouping]을 사용하는 것보다 비효율적이다 (Iterable vs Sequence 와 같다)
 *
 * @param keySelector 집계 기준이 되는 Key 선택 함수
 * @param aggregator 집계 함수
 * @return Key 기준의 집계 값의 Map
 */
inline fun <T: Any, K: Any, R: Any> Iterable<T>.groupingAggregate(
    crossinline keySelector: (T) -> K,
    @BuilderInference aggregator: (key: K, accumulator: R?, eleemnt: T, first: Boolean) -> R,
): Map<K, R> {
    return asSequence().groupingAggregate(keySelector, aggregator)
}

/**
 * Key 기준으로 그룹핑된 값의 갯수를 계산합니다.
 *
 * @param keySelector grouping 기준으로 사용할 key 선택자
 * @return key로 grouping 된 값들의 count를 집계한 값
 */
inline fun <T: Any, K: Any> Sequence<T>.groupingCount(crossinline keySelector: (T) -> K): Map<K, Int> =
    groupingBy(keySelector).eachCount()

/**
 * Key 기준으로 그룹핑된 값의 갯수를 계산합니다.
 *
 * @param keySelector grouping 기준으로 사용할 key 선택자
 * @return key로 grouping 된 값들의 count를 집계한 값
 */
inline fun <T: Any, K: Any> Iterable<T>.groupingCount(crossinline keySelector: (T) -> K): Map<K, Int> =
    asSequence().groupingCount(keySelector)

/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 * An initial value of accumulator is the same [initialValue] for each group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @see [Grouping.fold]
 */
inline fun <T: Any, K: Any, R: Any> Sequence<T>.groupingFold(
    crossinline keySelector: (T) -> K,
    initialValue: R,
    operation: (accumulator: R, element: T) -> R,
): Map<K, R> =
    groupingBy(keySelector).fold(initialValue, operation)

/**
 * Groups elements from the [Grouping] source by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 * An initial value of accumulator is the same [initialValue] for each group.
 *
 * @param operation a function that is invoked on each element with the following parameters:
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @see [Grouping.fold]
 */
inline fun <T: Any, K: Any, R: Any> Iterable<T>.groupingFold(
    crossinline keySelector: (T) -> K,
    initialValue: R,
    operation: (accumulator: R, element: T) -> R,
): Map<K, R> =
    asSequence().groupingFold(keySelector, initialValue, operation)

/**
 * Groups elements from the [Grouping] source by key and applies the reducing [operation] to the elements of each group
 * sequentially starting from the second element of the group,
 * passing the previously accumulated value and the current element as arguments,
 * and stores the results in a new map.
 * An initial value of accumulator is the first element of the group.
 *
 * @param operation a function that is invoked on each subsequent element of the group with the following parameters:
 *  - `key`: the key of the group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 * @see [Grouping.reduce]
 */
inline fun <T: S, K: Any, S: Any> Sequence<T>.groupingReduce(
    crossinline keySelector: (T) -> K,
    operation: (key: K, accumulator: S, element: T) -> S,
): Map<K, S> =
    groupingBy(keySelector).reduce(operation)

/**
 * Groups elements from the [Grouping] source by key and applies the reducing [operation] to the elements of each group
 * sequentially starting from the second element of the group,
 * passing the previously accumulated value and the current element as arguments,
 * and stores the results in a new map.
 * An initial value of accumulator is the first element of the group.
 *
 * @param operation a function that is invoked on each subsequent element of the group with the following parameters:
 *  - `key`: the key of the group this element belongs to;
 *  - `accumulator`: the current value of the accumulator of the group;
 *  - `element`: the element from the source being accumulated.
 *
 * @return a [Map] associating the key of each group with the result of accumulating the group elements.
 * @see [Grouping.reduce]
 */
inline fun <T: S, K: Any, S: Any> Iterable<T>.groupingReduce(
    crossinline keySelector: (T) -> K,
    operation: (key: K, accumulator: S, element: T) -> S,
): Map<K, S> =
    asSequence().groupingReduce(keySelector, operation)
