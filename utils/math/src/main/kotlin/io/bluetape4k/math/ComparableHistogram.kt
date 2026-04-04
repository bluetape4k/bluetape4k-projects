package io.bluetape4k.math

import io.bluetape4k.ranges.DefaultClosedClosedRange
import io.bluetape4k.ranges.DefaultClosedOpenRange
import io.bluetape4k.ranges.Range
import java.io.Serializable


/**
 * Histogram의 하나의 막대(구간)를 나타냅니다.
 *
 * ```kotlin
 * val bin = Bin(range = DefaultClosedClosedRange(0, 10), value = listOf(1, 5, 8))
 * val inBin = 5 in bin   // true
 * ```
 *
 * @param T 값의 타입
 * @param C 구간의 기준 타입 (Comparable)
 * @property range 막대가 나타내는 구간
 * @property value 해당 구간의 집계 값
 */
data class Bin<out T: Any, in C: Comparable<C>>(
    val range: Range<in C>,
    val value: T,
): Serializable {
    operator fun contains(key: C): Boolean = key in range
}

/**
 * Histogram 전체를 나타내는 막대 모음.
 *
 * ```kotlin
 * val model: BinModel<List<Int>, Int> = listOf(1, 3, 5, 7, 9).binByComparable(
 *     incrementer = { it + 2 },
 *     valueMapper = { it }
 * )
 * val bin = model[5]   // 5를 포함하는 구간의 Bin
 * ```
 */
data class BinModel<out T: Any, in C: Comparable<C>>(
    val bins: List<Bin<T, C>>,
): Iterable<Bin<T, C>> by bins, Serializable {
    operator fun get(key: C): Bin<T, C>? = bins.find { key in it.range }
    operator fun contains(key: C) = bins.any { key in it.range }
}

/**
 * Sequence를 Comparable 기준으로 히스토그램 구간으로 분류합니다.
 *
 * ```kotlin
 * val data = sequenceOf(1, 3, 5, 7, 9)
 * val model = data.binByComparable(incrementer = { it + 2 }, valueMapper = { it })
 * // model 은 [1,3], [3,5], [5,7], [7,9] 구간의 BinModel
 * ```
 */
inline fun <T: Any, C: Comparable<C>> Sequence<T>.binByComparable(
    incrementer: (C) -> C,
    valueMapper: (T) -> C,
    rangeStart: C? = null,
): BinModel<List<T>, C> =
    asIterable().binByComparable(incrementer, valueMapper, rangeStart)

/**
 * Iterable을 Comparable 기준으로 히스토그램 구간으로 분류합니다.
 *
 * ```kotlin
 * val data = listOf(1, 3, 5, 7, 9)
 * val model = data.binByComparable(incrementer = { it + 2 }, valueMapper = { it })
 * // model 은 [1,3], [3,5], [5,7], [7,9] 구간의 BinModel
 * ```
 */
inline fun <T: Any, C: Comparable<C>> Iterable<T>.binByComparable(
    incrementer: (C) -> C,
    valueMapper: (T) -> C,
    rangeStart: C? = null,
): BinModel<List<T>, C> =
    binByComparable(incrementer, valueMapper, { it }, rangeStart)

/**
 * Histogram 을 만듭니다.
 *
 * ```kotlin
 * val data = listOf(1, 3, 5, 7, 9)
 * val model = data.binByComparable(
 *     incrementer = { it + 2 },
 *     valueMapper = { it },
 *     groupOp = { items -> items.size }
 * )
 * // model 은 구간별 항목 수를 담은 BinModel<Int, Int>
 * ```
 *
 * @param T 원본 데이터 타입
 * @param C 구간의 기준 타입 (Comparable)
 * @param incrementer   값 증가 값
 * @param valueMapper   Value mapper
 * @param groupOp       grouping operator (eg: count or max)
 * @param rangeStart    막대의 시작 시점 (null 이면 value mapper의 최소값을 기준으로 합니다)
 */
inline fun <T: Any, C: Comparable<C>, G: Any> Iterable<T>.binByComparable(
    incrementer: (C) -> C,
    valueMapper: (T) -> C,
    crossinline groupOp: (List<T>) -> G,
    rangeStart: C? = null,
    endExclusive: Boolean = false,
): BinModel<G, C> {
    assert(count() > 0) { "Collection must not be empty." }

    val groupByC: MutableMap<C, MutableList<T>> = mutableMapOf()
    this.groupByTo(groupByC, valueMapper)

    val minC: C = rangeStart ?: groupByC.keys.minOrNull()!!
    val maxC: C = groupByC.keys.maxOrNull()!!

    // Histogram의 막대의 컬렉션을 구성합니다.
    val bins = mutableListOf<Range<C>>()
        .apply {
            val rangeFactory = { lowerBound: C, upperBound: C ->
                if (endExclusive) DefaultClosedOpenRange(lowerBound, upperBound)
                else DefaultClosedClosedRange(lowerBound, upperBound)
            }

            var currentRangeStart = minC
            var currentRangeEnd = minC
            while (currentRangeEnd < maxC) {
                currentRangeEnd = incrementer(currentRangeEnd)
                add(rangeFactory(currentRangeStart, currentRangeEnd))
                currentRangeStart = currentRangeEnd
            }
        }

    return bins
        .map { range ->
            val binWithList = range to mutableListOf<T>()
            groupByC.entries
                .asSequence()
                .filter {
                    it.key in binWithList.first
                }
                .forEach {
                    binWithList.second.addAll(it.value)
                }

            Bin(binWithList.first, groupOp(binWithList.second))
        }
        .let(::BinModel)
}
