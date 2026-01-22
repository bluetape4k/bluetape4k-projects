package io.bluetape4k.math

import io.bluetape4k.ranges.DefaultClosedClosedRange
import io.bluetape4k.ranges.DefaultClosedOpenRange
import io.bluetape4k.ranges.Range
import java.io.Serializable


/**
 * Histgram의 하나의 막대를 나타냅니다.
 *
 * @param T
 * @param C
 * @property range
 * @property value
 * @constructor Create empty Bin
 */
data class Bin<out T: Any, in C: Comparable<C>>(
    val range: Range<in C>,
    val value: T,
): Serializable {
    operator fun contains(key: C): Boolean = key in range
}

data class BinModel<out T: Any, in C: Comparable<C>>(
    val bins: List<Bin<T, C>>,
): Iterable<Bin<T, C>> by bins, Serializable {
    operator fun get(key: C): Bin<T, C>? = bins.find { key in it.range }
    operator fun contains(key: C) = bins.any { key in it.range }
}

inline fun <T: Any, C: Comparable<C>> Sequence<T>.binByComparable(
    incrementer: (C) -> C,
    valueMapper: (T) -> C,
    rangeStart: C? = null,
): BinModel<List<T>, C> =
    asIterable().binByComparable(incrementer, valueMapper, rangeStart)

inline fun <T: Any, C: Comparable<C>> Iterable<T>.binByComparable(
    incrementer: (C) -> C,
    valueMapper: (T) -> C,
    rangeStart: C? = null,
): BinModel<List<T>, C> =
    binByComparable(incrementer, valueMapper, { it }, rangeStart)

/**
 * Histogram 을 만듭니다.
 *
 * @param T
 * @param C
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

    val groupByC: Map<C, List<T>> = asSequence().groupBy(valueMapper)
    val minC: C = rangeStart ?: groupByC.keys.minOrNull()!!
    val maxC: C = groupByC.keys.maxOrNull()!!

    // Histogram의 막대의 컬렉션을 구성합니다.
    val bins = mutableListOf<Range<C>>().apply {
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

    return bins.asSequence()
        .map { range ->
            val binWithList = range to mutableListOf<T>()
            groupByC.entries.asSequence()
                .filter {
                    it.key in binWithList.first
                }
                .forEach {
                    binWithList.second.addAll(it.value)
                }

            Bin(binWithList.first, groupOp(binWithList.second))
        }
        .toList()
        .let(::BinModel)
}
