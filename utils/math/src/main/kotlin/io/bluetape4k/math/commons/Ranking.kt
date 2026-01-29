package io.bluetape4k.math.commons

import io.bluetape4k.collections.eclipse.toFastList
import org.apache.commons.math3.stat.ranking.NaNStrategy
import org.apache.commons.math3.stat.ranking.NaturalRanking
import org.apache.commons.math3.stat.ranking.TiesStrategy

/**
 * 요소의 순위를 계산합니다.
 * 가장 높은 수가 rank = 0 입니다.
 *
 * @param nanStrategy 값이 NaN 일 경우의 ranking 전략 (기본: 최소값으로 ranking)
 * @param tiesStrategy 값이 같을 경우 순위 전략 (기본: 높은 순위로 지정)
 * @return 요소별 순위 정보
 */
fun <T> Sequence<T>.ranking(
    nanStrategy: NaNStrategy = NaNStrategy.MINIMAL,
    tiesStrategy: TiesStrategy = TiesStrategy.MAXIMUM,
): Map<T, Int> where T: Number, T: Comparable<T> {
    val ranks = NaturalRanking(nanStrategy, tiesStrategy)
        .rank(map { it.toDouble() }.toFastList().toDoubleArray())
        .map { it.toInt() }

    return mapIndexed { index, item -> item to ranks.size - ranks[index] }.toMap()
}

/**
 * 요소의 순위를 계산합니다.
 * 가장 높은 수가 rank = 0 입니다.
 *
 * @param nanStrategy 값이 NaN 일 경우의 ranking 전략 (기본: 최소값으로 ranking)
 * @param tiesStrategy 값이 같을 경우 순위 전략 (기본: 높은 순위로 지정)
 * @return 요소별 순위 정보
 */
fun <T> Iterable<T>.ranking(
    nanStrategy: NaNStrategy = NaNStrategy.MINIMAL,
    tiesStrategy: TiesStrategy = TiesStrategy.MAXIMUM,
): Map<T, Int> where T: Number, T: Comparable<T> {
    return asSequence().ranking(nanStrategy, tiesStrategy)
}

/**
 * 요소들의 특정 값(V)을 기준으로 순위를 매깁니다.
 * 가장 높은 수가 rank = 0 입니다.
 *
 * @param valueSelector 순위를 매길 값을 선택할 수 있도록 한다
 * @param nanStrategy 값이 NaN 일 경우의 ranking 전략 (기본: 최소값으로 ranking)
 * @param tiesStrategy 값이 같을 경우 순위 전략 (기본: 높은 순위로 지정)
 * @return 요소별 순위 정보
 */
inline fun <T, V> Sequence<T>.ranking(
    nanStrategy: NaNStrategy = NaNStrategy.MINIMAL,
    tiesStrategy: TiesStrategy = TiesStrategy.MAXIMUM,
    crossinline valueSelector: (T) -> V,
): Map<T, Int> where V: Number, V: Comparable<V> {
    val ranks: Map<V, Int> = this.map { valueSelector(it) }.ranking(nanStrategy, tiesStrategy)
    return this.associateWith { ranks[valueSelector(it)]!! }
}

/**
 * 요소들의 특정 값(V)을 기준으로 순위를 매깁니다.
 * 가장 높은 수가 rank = 0 입니다.
 *
 * @param valueSelector 순위를 매길 값을 선택할 수 있도록 한다
 * @param nanStrategy 값이 NaN 일 경우의 ranking 전략 (기본: 최소값으로 ranking)
 * @param tiesStrategy 값이 같을 경우 순위 전략 (기본: 높은 순위로 지정)
 * @return 요소별 순위 정보
 */
inline fun <T, V> Iterable<T>.ranking(
    nanStrategy: NaNStrategy = NaNStrategy.MINIMAL,
    tiesStrategy: TiesStrategy = TiesStrategy.MAXIMUM,
    crossinline valueSelector: (T) -> V,
): Map<T, Int> where V: Number, V: Comparable<V> {
    return asSequence().ranking(nanStrategy, tiesStrategy, valueSelector)
}
