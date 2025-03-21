package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.ValueObject
import io.bluetape4k.ahocorasick.interval.IntervalableComparators.PositionComparator
import io.bluetape4k.ahocorasick.interval.IntervalableComparators.ReverseSizeComparator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace


class IntervalTree private constructor(private val rootNode: IntervalNode): ValueObject {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(rootNode: IntervalNode): IntervalTree {
            return IntervalTree(rootNode)
        }

        @JvmStatic
        operator fun invoke(intervals: List<Intervalable>): IntervalTree {
            return invoke(IntervalNode(intervals))
        }
    }

    fun findOverlaps(interval: Intervalable): List<Intervalable> {
        return rootNode.findOverlaps(interval).toList().sortedWith(PositionComparator)
    }

    fun <T: Intervalable> removeOverlaps(intervals: Collection<T>): MutableList<T> {
        // size가 큰 것부터
        val results = intervals.sortedWith(ReverseSizeComparator).toMutableList()
        val removed = mutableSetOf<Intervalable>()

        // 꼭 Sequence 방식으로 수행해야 updated된 removed를 사용할 수 있습니다.
        results.asSequence()
            .filterNot { removed.contains(it) }
            .forEach { target ->
                val overlaps = findOverlaps(target)
                log.trace { "target=$target, overlaps=$overlaps" }
                removed.addAll(overlaps)
            }

        // overlap 된 interval들을 삭제합니다.
        log.trace { "overlap 된 interval들을 삭제=$removed" }
        results.removeAll(removed)

        // sort the intervals, now on left-most position only
        return results.sortedWith(PositionComparator).toMutableList()
    }
}
