package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendarConfig
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
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

    @Test
    fun `mapped and unmapped start end should be stable`() {
        val start = nowZonedDateTime()
        val end = start + Duration.ofHours(2)
        val calendar = TimeCalendar(
            TimeCalendarConfig(
                startOffset = Duration.ofHours(1),
                endOffset = Duration.ofHours(-1),
            )
        )

        val range = CalendarTimeRange(TimeRange(start, end), calendar)

        range.start shouldBeEqualTo start + Duration.ofHours(1)
        range.end shouldBeEqualTo end + Duration.ofHours(-1)
        range.mappedStart shouldBeEqualTo range.start
        range.mappedEnd shouldBeEqualTo range.end
        range.unmappedStart shouldBeEqualTo start
        range.unmappedEnd shouldBeEqualTo end
    }
}
