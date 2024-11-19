package io.bluetape4k.math.commons

import io.bluetape4k.math.MathConsts.Pi
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 해당 평균, 표준편차를 가지는 정규분포에서의 x 지점에서의 확률을 구한다.
 *
 * @param avg 평균
 * @param stdev 표준편차
 * @return 정규분포 상의 확률
 */
fun Double.normalDensity(avg: Double, stdev: Double): Double =
    exp(-(((this - avg) / stdev).pow(2.0) / 2.0)) / sqrt(2.0 * Pi) / stdev

/**
 * 해당 평균, 표준편차를 가지는 정규분포에서의 x 지점에서의 확률을 구한다.
 *
 * @param avg 평균
 * @param stdev 표준편차
 * @return
 */
fun Sequence<Double>.normalDensity(avg: Double, stdev: Double): Sequence<Double> =
    map { it.normalDensity(avg, stdev) }


/**
 * 컬렉션 각 요소와 빈도 수를 반환합니다.
 */
fun <T> Sequence<T>.frequency(): Map<T, Int> {
    return groupingBy { it }.eachCount()
}

fun <T> Iterable<T>.frequency(): Map<T, Int> {
    return groupingBy { it }.eachCount()
}

/**
 * 컬렉션 요소의 `selector` 기준으로 빈도 수를 계산합니다
 */
fun <T, V> Sequence<T>.frequency(selector: (T) -> V): Map<T, Int> {
    val frequency: Map<V, Int> = map { selector(it) }.frequency()
    val inverse: MutableMap<V, MutableList<T>> = mutableMapOf()

    this
        .distinct()
        .map { it to selector(it) }
        .forEach { (t: T, v: V) ->
            inverse.computeIfAbsent(v) { mutableListOf() }.add(t)
        }

    val result: MutableMap<T, Int> = mutableMapOf()
    frequency.forEach { (v: V, count: Int) ->
        inverse
            .computeIfAbsent(v) { mutableListOf() }
            .forEach { t: T ->
                result.compute(t) { _, cnt -> (cnt ?: 0) + count }
            }
    }
    return result
}

fun <T, V> Iterable<T>.frequency(selector: (T) -> V): Map<T, Int> {
    return asSequence().frequency(selector)
}

/**
 * 항목 `item`의 빈도 수 / 전체 항목 수
 *
 * @param item 비교 항목
 * @param biPredicate 비교 함수
 * @return 항목 수 / 전체 항목 수
 */
fun <T> Sequence<T>.probability(item: T, biPredicate: (T, T) -> Boolean): Double {
    val matches = count { biPredicate(it, item) }
    return matches.toDouble() / this.count().toDouble()
}

/**
 * 항목 `item`의 빈도 수 / 전체 항목 수
 *
 * @param item 비교 항목
 * @param biPredicate 비교 함수
 * @return 항목 수 / 전체 항목 수
 */
fun <T> Iterable<T>.probability(item: T, biPredicate: (T, T) -> Boolean): Double {
    return asSequence().probability(item, biPredicate)
}
