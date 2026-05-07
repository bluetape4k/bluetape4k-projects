package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.HoursPerDay
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class DayTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `default constructor uses today with dayCount 1`() {
        val range = DayTimeRange()
        range.dayCount shouldBeEqualTo 1
    }

    @Test
    fun `dayCount property is correct`() {
        val range = DayTimeRange(now, dayCount = 3)
        range.dayCount shouldBeEqualTo 3
    }

    @Test
    fun `hours returns HoursPerDay times dayCount elements`() {
        val range = DayTimeRange(now, dayCount = 2)
        range.hours() shouldHaveSize 2 * HoursPerDay
    }

    @Test
    fun `hourSequence returns lazy sequence`() {
        val range = DayTimeRange(now, dayCount = 1)
        range.hourSequence().count() shouldBeEqualTo HoursPerDay
    }

    @Test
    fun `minutes returns 60 times hours elements`() {
        val range = DayTimeRange(now, dayCount = 1)
        range.minutes() shouldHaveSize HoursPerDay * 60
    }

    @Test
    fun `start is beginning of day`() {
        val refDate = zonedDateTimeOf(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = DayTimeRange(refDate, dayCount = 1)
        range.start.hour shouldBeEqualTo 0
        range.start.minute shouldBeEqualTo 0
        range.start.second shouldBeEqualTo 0
    }

    @Test
    fun `end is approximately start plus dayCount days`() {
        val refDate = zonedDateTimeOf(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC)
        val range = DayTimeRange(refDate, dayCount = 3)
        // Default calendar applies -1ns endOffset, so end is June17 23:59:59.999... or June18 depending on offset
        // The important thing is the duration spans 3 days
        val durationDays = java.time.Duration.between(range.start, range.end.plusNanos(1)).toDays()
        durationDays shouldBeEqualTo 3L
    }

    @Test
    fun `dayCount 7 has 7 days worth of hours`() {
        val range = DayTimeRange(now, dayCount = 7)
        range.hours() shouldHaveSize 7 * HoursPerDay
    }
}
