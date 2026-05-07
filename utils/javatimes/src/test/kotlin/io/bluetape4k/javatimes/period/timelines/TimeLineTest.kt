package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimePeriodCollection
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test

class TimeLineTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `empty periods returns empty combinePeriods`() {
        val container = TimePeriodCollection()
        val timeLine = TimeLine<ITimePeriod>(container)
        val result = timeLine.combinePeriods()
        result.shouldBeEmpty()
    }

    @Test
    fun `single period returns itself in combinePeriods`() {
        val start = zonedDateTimeOf(2024, 1, 1)
        val end = zonedDateTimeOf(2024, 1, 10)
        val container = TimePeriodCollection(TimeRange(start, end))
        val timeLine = TimeLine<ITimePeriod>(container)
        val result = timeLine.combinePeriods()

        result shouldHaveSize 1
        result.periods.first().start shouldBeEqualTo start
        result.periods.first().end shouldBeEqualTo end
    }

    @Test
    fun `overlapping periods are combined`() {
        val container = TimePeriodCollection()
        container.add(TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 10)))
        container.add(TimeRange(zonedDateTimeOf(2024, 1, 5), zonedDateTimeOf(2024, 1, 15)))

        val timeLine = TimeLine<ITimePeriod>(container)
        val result = timeLine.combinePeriods()

        result shouldHaveSize 1
        result.periods.first().start shouldBeEqualTo zonedDateTimeOf(2024, 1, 1)
        result.periods.first().end shouldBeEqualTo zonedDateTimeOf(2024, 1, 15)
    }

    @Test
    fun `calculateGaps returns gaps between periods`() {
        val container = TimePeriodCollection()
        val p1 = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 5))
        val p2 = TimeRange(zonedDateTimeOf(2024, 1, 10), zonedDateTimeOf(2024, 1, 15))
        container.add(p1)
        container.add(p2)

        val timeLine = TimeLine<ITimePeriod>(container)
        val gaps = timeLine.calculateGaps()

        gaps shouldHaveSize 1
        gaps.periods.first().start shouldBeEqualTo zonedDateTimeOf(2024, 1, 5)
        gaps.periods.first().end shouldBeEqualTo zonedDateTimeOf(2024, 1, 10)
    }

    @Test
    fun `calculateGaps with no gaps returns empty`() {
        val container = TimePeriodCollection()
        val p1 = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 10))
        val p2 = TimeRange(zonedDateTimeOf(2024, 1, 5), zonedDateTimeOf(2024, 1, 20))
        container.add(p1)
        container.add(p2)

        val timeLine = TimeLine<ITimePeriod>(container)
        val gaps = timeLine.calculateGaps()
        gaps.shouldBeEmpty()
    }

    @Test
    fun `intersectPeriods returns intersection of overlapping periods`() {
        val container = TimePeriodCollection()
        container.add(TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 10)))
        container.add(TimeRange(zonedDateTimeOf(2024, 1, 5), zonedDateTimeOf(2024, 1, 15)))

        val timeLine = TimeLine<ITimePeriod>(container)
        val intersections = timeLine.intersectPeriods()

        intersections shouldHaveSize 1
        intersections.periods.first().start shouldBeEqualTo zonedDateTimeOf(2024, 1, 5)
        intersections.periods.first().end shouldBeEqualTo zonedDateTimeOf(2024, 1, 10)
    }

    @Test
    fun `limits defaults to bounding range of all periods`() {
        val container = TimePeriodCollection()
        val start = zonedDateTimeOf(2024, 1, 1)
        val end = zonedDateTimeOf(2024, 1, 31)
        container.add(TimeRange(start, end))

        val timeLine = TimeLine<ITimePeriod>(container)
        timeLine.limits.start shouldBeEqualTo start
        timeLine.limits.end shouldBeEqualTo end
    }

    @Test
    fun `custom limits restrict gap calculation`() {
        val container = TimePeriodCollection()
        container.add(TimeRange(zonedDateTimeOf(2024, 1, 5), zonedDateTimeOf(2024, 1, 10)))

        val limits = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 15))
        val timeLine = TimeLine<ITimePeriod>(container, limits)
        val gaps = timeLine.calculateGaps()

        gaps shouldHaveSize 2
    }
}
