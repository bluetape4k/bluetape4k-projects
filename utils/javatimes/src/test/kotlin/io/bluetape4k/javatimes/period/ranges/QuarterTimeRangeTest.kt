package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MonthsPerQuarter
import io.bluetape4k.javatimes.Quarter
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class QuarterTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `default constructor uses today with quarterCount 1`() {
        val range = QuarterTimeRange()
        range.quarterCount shouldBeEqualTo 1
    }

    @Test
    fun `quarterCount property is correct`() {
        val range = QuarterTimeRange(now, quarterCount = 2)
        range.quarterCount shouldBeEqualTo 2
    }

    @Test
    fun `quarterOfStart is Q1 for January`() {
        val janDate = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = QuarterTimeRange(janDate, quarterCount = 1)
        range.quarterOfStart shouldBeEqualTo Quarter.Q1
    }

    @Test
    fun `quarterOfStart is Q2 for April`() {
        val aprDate = zonedDateTimeOf(2024, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = QuarterTimeRange(aprDate, quarterCount = 1)
        range.quarterOfStart shouldBeEqualTo Quarter.Q2
    }

    @Test
    fun `months returns MonthsPerQuarter times quarterCount`() {
        val range = QuarterTimeRange(now, quarterCount = 2)
        range.months() shouldHaveSize 2 * MonthsPerQuarter
    }

    @Test
    fun `monthSequence returns lazy sequence`() {
        val range = QuarterTimeRange(now, quarterCount = 1)
        range.monthSequence().count() shouldBeEqualTo MonthsPerQuarter
    }

    @Test
    fun `isMultipleCalendarYears is false for single quarter in same year`() {
        val janDate = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = QuarterTimeRange(janDate, quarterCount = 1)
        range.isMultipleCalendarYears.shouldBeFalse()
    }

    @Test
    fun `isMultipleCalendarYears is true for range crossing year boundary`() {
        val q4Date = zonedDateTimeOf(2024, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = QuarterTimeRange(q4Date, quarterCount = 2)
        range.isMultipleCalendarYears.shouldBeTrue()
    }

    @Test
    fun `days returns all DayRanges in all months`() {
        val q1Date = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = QuarterTimeRange(q1Date, quarterCount = 1)
        // Q1 2024: Jan(31) + Feb(29, leap) + Mar(31) = 91
        range.days().size shouldBeEqualTo 91
    }
}
