package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.ahocorasick.interval.IntervalableComparators.ReverseSizeComparator
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class IntervalableComparatorTest {

    companion object: KLogging()

    @Test
    fun `compare intervalable by position`() {
        val intervals = fastListOf(
            Interval(4, 5),
            Interval(1, 4),
            Interval(3, 8)
        )

        intervals.sortWith(IntervalableComparators.PositionComparator)

        intervals[0] shouldBeEqualTo Interval(1, 4)
        intervals[1] shouldBeEqualTo Interval(3, 8)
        intervals[2] shouldBeEqualTo Interval(4, 5)
    }

    @Test
    fun `compare intervalable by size`() {
        val intervals = fastListOf(
            Interval(4, 5),
            Interval(1, 4),
            Interval(3, 8)
        )

        intervals.sortWith(IntervalableComparators.SizeComparator)

        intervals[0].size shouldBeEqualTo 2
        intervals[1].size shouldBeEqualTo 4
        intervals[2].size shouldBeEqualTo 6
    }

    @Test
    fun `compare intervalable by size reverse`() {
        val intervals = fastListOf(
            Interval(4, 5),
            Interval(1, 4),
            Interval(3, 8)
        )

        intervals.sortWith(ReverseSizeComparator)

        intervals.map { it.size } shouldBeEqualTo fastListOf(6, 4, 2)
    }

    @Test
    fun `compare intervalable by size reverse and position`() {
        val intervals = fastListOf(
            Interval(4, 7),
            Interval(2, 5),
            Interval(3, 6)
        )

        intervals.sortedWith(ReverseSizeComparator) shouldBeEqualTo fastListOf(
            Interval(2, 5),
            Interval(3, 6),
            Interval(4, 7)
        )
    }
}
