package io.bluetape4k.javatimes.period.ranges.coroutines

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendarConfig
import io.bluetape4k.javatimes.period.ranges.CalendarTimeRange
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.javatimes.period.ranges.HourRange
import io.bluetape4k.javatimes.period.ranges.MinuteRange
import io.bluetape4k.javatimes.period.ranges.MonthRange
import io.bluetape4k.javatimes.period.ranges.YearRange
import io.bluetape4k.javatimes.zonedDateTimeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class RangeFlowsTest {

    private val start = zonedDateTimeOf(2025, 3, 15, 10, 30)
    private val calendar = TimeCalendar(
        TimeCalendarConfig(
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )
    )

    @Test
    fun `flowOfYearRange emits aligned ranges with calendar`() = runTest {
        assertRangeFlow(
            count = 3,
            factory = ::flowOfYearRange,
            expectedFirst = { time, cal -> YearRange(time, cal) },
        )
    }

    @Test
    fun `flowOfMonthRange emits aligned ranges with calendar`() = runTest {
        assertRangeFlow(
            count = 4,
            factory = ::flowOfMonthRange,
            expectedFirst = { time, cal -> MonthRange(time, cal) },
        )
    }

    @Test
    fun `flowOfWeekRange honors calendar firstDayOfWeek`() = runTest {
        val items = flowOfWeekRange(start, 5, calendar).toList()

        items.size shouldBeEqualTo 5
        items.first().unmappedStart shouldBeEqualTo zonedDateTimeOf(2025, 3, 9, zoneId = start.zone)
        items.first().calendar shouldBeEqualTo calendar
    }

    @Test
    fun `flowOfDayRange emits aligned ranges with calendar`() = runTest {
        assertRangeFlow(
            count = 6,
            factory = ::flowOfDayRange,
            expectedFirst = { time, cal -> DayRange(time, cal) },
        )
    }

    @Test
    fun `flowOfHourRange emits aligned ranges with calendar`() = runTest {
        assertRangeFlow(
            count = 7,
            factory = ::flowOfHourRange,
            expectedFirst = { time, cal -> HourRange(time, cal) },
        )
    }

    @Test
    fun `flowOfMinuteRange emits aligned ranges with calendar`() = runTest {
        assertRangeFlow(
            count = 8,
            factory = ::flowOfMinuteRange,
            expectedFirst = { time, cal -> MinuteRange(time, cal) },
        )
    }

    private suspend fun <T: CalendarTimeRange> assertRangeFlow(
        count: Int,
        factory: (startTime: java.time.ZonedDateTime, count: Int, calendar: ITimeCalendar) -> Flow<T>,
        expectedFirst: (startTime: java.time.ZonedDateTime, calendar: ITimeCalendar) -> T,
    ) {
        val items = factory(start, count, calendar).toList()

        items.size shouldBeEqualTo count
        items.first() shouldBeEqualTo expectedFirst(start, calendar)
        items.forEach { it.calendar shouldBeEqualTo calendar }
    }
}
