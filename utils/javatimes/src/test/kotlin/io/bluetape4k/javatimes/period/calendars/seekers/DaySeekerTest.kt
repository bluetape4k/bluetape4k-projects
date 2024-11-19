package io.bluetape4k.javatimes.period.calendars.seekers

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.SeekDirection
import io.bluetape4k.javatimes.period.calendars.CalendarVisitorFilter
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.javatimes.period.ranges.DayRangeCollection
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class DaySeekerTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `simple forward seeker`() = runTest {
        val start = DayRange()
        val daySeeker = DaySeeker()

        val day1 = daySeeker.findDay(start, 0)
        day1 shouldBeEqualTo start

        val day2 = daySeeker.findDay(start, 1)
        day2 shouldBeEqualTo start.nextDay()

        (-10..20).forEach { i ->
            val offset = i * 5
            val day = daySeeker.findDay(start, offset)
            day shouldBeEqualTo start.addDays(offset)
        }
    }

    @Test
    fun `simple backward seeker`() = runTest {
        val start = DayRange()
        val daySeeker = DaySeeker(CalendarVisitorFilter(), SeekDirection.BACKWARD)

        val day1 = daySeeker.findDay(start, 0)
        day1 shouldBeEqualTo start

        val day2 = daySeeker.findDay(start, 1)
        day2 shouldBeEqualTo start.prevDay()

        (-10..20).forEach { i ->
            val offset = i * 5
            val day = daySeeker.findDay(start, offset)
            day shouldBeEqualTo start.addDays(-offset)
        }
    }

    @Test
    fun `various SeekDirection`() = runTest {
        val start = DayRange()
        val daySeeker = DaySeeker()

        (-10..20).forEach {
            val offset = it * 5
            val day = daySeeker.findDay(start, offset)
            day shouldBeEqualTo start.addDays(offset)
        }

        val backwardSeeker = DaySeeker(CalendarVisitorFilter(), SeekDirection.BACKWARD)

        (-10..20).forEach {
            val offset = it * 5
            val day = backwardSeeker.findDay(start, offset)
            day shouldBeEqualTo start.addDays(-offset)
        }
    }

    @Test
    fun `minimum date`() = runTest {
        val daySeeker = DaySeeker()
        val day = daySeeker.findDay(DayRange(MinPeriodTime), -10)
        day.shouldBeNull()
    }

    @Test
    fun `maximum date`() = runTest {
        val daySeeker = DaySeeker()
        val day = daySeeker.findDay(DayRange(MaxPeriodTime - 1.days()), 10)
        day.shouldBeNull()
    }

    @Test
    fun `seek with exclude periods`() = runTest {
        val start = DayRange(2011, 2, 15)

        val filter = CalendarVisitorFilter().apply {
            addWorkingWeekdays()

            // 14 days -> week 9 and week 10
            excludePeriods.add(
                DayRangeCollection(
                    zonedDateTimeOf(2011, 2, 27),
                    14
                )
            )
        }

        val daySeeker = DaySeeker(filter)

        val day1 = daySeeker.findDay(start, 3)
        day1 shouldBeEqualTo DayRange(2011, 2, 18)

        val day2 = daySeeker.findDay(start, 4)   // 주말 (19, 20) 제외
        day2 shouldBeEqualTo DayRange(2011, 2, 21)

        val day3 = daySeeker.findDay(start, 10)   // 주말 (19, 20) 제외, 2.27부터 14일간 휴가
        day3 shouldBeEqualTo DayRange(2011, 3, 15)
    }
}
