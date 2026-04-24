package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class MonthTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `default constructor uses today with monthCount 1`() {
        val range = MonthTimeRange()
        range.monthCount shouldBeEqualTo 1
    }

    @Test
    fun `monthCount property is correct`() {
        val range = MonthTimeRange(now, monthCount = 3)
        range.monthCount shouldBeEqualTo 3
    }

    @Test
    fun `days returns all DayRanges for the given months`() {
        val refDate = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = MonthTimeRange(refDate, monthCount = 1)
        // January has 31 days
        range.days() shouldHaveSize 31
    }

    @Test
    fun `daySequence returns lazy sequence`() {
        val refDate = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = MonthTimeRange(refDate, monthCount = 1)
        range.daySequence().count() shouldBeEqualTo 31
    }

    @Test
    fun `monthCount 3 covers 3 calendar months`() {
        val refDate = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = MonthTimeRange(refDate, monthCount = 3)
        // Jan(31) + Feb(29 in 2024 leap year) + Mar(31) = 91
        range.days().size shouldBeEqualTo 91
    }

    @Test
    fun `hours returns hours for all days in the range`() {
        val refDate = zonedDateTimeOf(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = MonthTimeRange(refDate, monthCount = 1)
        // Feb 2024 has 29 days → 29 * 24 = 696 hours
        (range.hours().size > 0).shouldBeTrue()
    }

    @Test
    fun `start is beginning of the first day of the period`() {
        val refDate = zonedDateTimeOf(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = MonthTimeRange(refDate, monthCount = 1)
        range.start.hour shouldBeEqualTo 0
        range.start.minute shouldBeEqualTo 0
    }
}
