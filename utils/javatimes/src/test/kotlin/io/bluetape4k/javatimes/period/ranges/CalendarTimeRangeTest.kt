package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CalendarTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `construct with calendar`() {
        val calendar = TimeCalendar()
        val range = CalendarTimeRange(TimeRange.AnyTime, calendar)

        range.calendar shouldBeEqualTo calendar
        range.isAnyTime.shouldBeTrue()
    }

    @Test
    fun `construct with moment`() {
        val today = nowZonedDateTime()

        assertFailsWith<AssertionError> {
            CalendarTimeRange(today, today)
        }
    }
}
