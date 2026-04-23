package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimePeriod
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class YearCalendarTimeRangeTest {

    companion object : KLogging()

    @Test
    fun `default constructor uses AnyTime period and Default calendar`() {
        val range = YearCalendarTimeRange()
        range.shouldNotBeNull()
    }

    @Test
    fun `baseYear from current year`() {
        val now = nowZonedDateTime()
        val yearRange = YearRange(now, TimeCalendar.EmptyOffset)
        val range = YearCalendarTimeRange(yearRange, TimeCalendar.EmptyOffset)
        range.baseYear shouldBeEqualTo now.year
    }

    @Test
    fun `baseYear with specific year period`() {
        val start = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, nowZonedDateTime().zone)
        val yearRange = YearRange(start, TimeCalendar.EmptyOffset)
        val range = YearCalendarTimeRange(yearRange, TimeCalendar.EmptyOffset)
        range.baseYear shouldBeEqualTo 2023
    }

    @Test
    fun `startYear and endYear are accessible`() {
        val now = nowZonedDateTime()
        val yearRange = YearRange(now, TimeCalendar.EmptyOffset)
        val range = YearCalendarTimeRange(yearRange, TimeCalendar.EmptyOffset)
        range.startYear shouldBeEqualTo now.year
        range.endYear shouldBeEqualTo now.year + 1
    }

    @Test
    fun `calendar property is accessible`() {
        val range = YearCalendarTimeRange(TimePeriod.AnyTime, TimeCalendar.Default)
        range.calendar shouldBeEqualTo TimeCalendar.Default
    }

    @Test
    fun `AnyTime period creates valid range`() {
        val range = YearCalendarTimeRange(TimePeriod.AnyTime, TimeCalendar.Default)
        range.shouldNotBeNull()
    }
}
