package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.nanos
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.SeekBoundaryMode
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.javatimes.period.ranges.HourRangeInDay
import io.bluetape4k.javatimes.unaryMinus
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import java.time.LocalTime


class CalendarDateAddTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `no period`() = runTest {
        val dateAdd = CalendarDateAdd()
        val now = nowZonedDateTime()

        (-10..20).forEach {
            val offset = it * 5

            dateAdd.add(now, offset.days()) shouldBeEqualTo now + offset.days()
            dateAdd.add(now, -offset.days()) shouldBeEqualTo now - offset.days()

            dateAdd.subtract(now, offset.days()) shouldBeEqualTo now - offset.days()
            dateAdd.subtract(now, -offset.days()) shouldBeEqualTo now + offset.days()
        }
    }

    @Test
    fun `period with limits`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 20),
            zonedDateTimeOf(2011, 4, 25)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 4, 30),
            null
        )   // 4월 30일 이후

        val dateAdd = CalendarDateAdd().apply {
            // 예외 기간을 설정합니다.
            excludePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start + 1.days()

        // 4월 12일에 8일을 더하면 4월 20일이지만, 20~25일까지 제외되므로, 4월 25일이 된다.
        dateAdd.add(start, 8.days()) shouldBeEqualTo period1.end

        // 4월 12에 20일을 더하면 4월 20~25일을 제외한 후 계산하면 4월 30 이후가 된다. (5월 3일).
        // 하지만 4월 30 이후는 모두 제외되므로 결과값은 null이다.
        dateAdd.add(start, 20.days()).shouldBeNull()

        dateAdd.subtract(start, 3.days()) shouldBeEqualTo start - 3.days()
    }

    @Test
    fun `period subtract with limit`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 30)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 20),
            zonedDateTimeOf(2011, 4, 25)
        )
        val period2 = TimeRange(
            null,
            zonedDateTimeOf(2011, 4, 6)
        )   // 4월 6일 까지

        val dateAdd = CalendarDateAdd().apply {
            // 예외 기간을 설정합니다.
            excludePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.subtract(start, 1.days()) shouldBeEqualTo start - 1.days()

        // 4월 30일로부터 5일 전이면 4월 25일이지만, 예외기간이므로 4월 20일이 된다.
        dateAdd.subtract(start, 5.days()) shouldBeEqualTo period1.start

        // 4월 30일로부터 20일 전이면, 5월 전이 4월20일이므로, 4월 5일이 된다. 근데, 4월 6일 이전은 모두 제외기간이므로 null을 반환한다.
        dateAdd.subtract(start, 20.days()).shouldBeNull()
    }

    @Test
    fun `one exclude period`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period = TimeRange(
            zonedDateTimeOf(2011, 4, 15),
            zonedDateTimeOf(2011, 4, 20)
        )

        val dateAdd = CalendarDateAdd().apply {
            excludePeriods.add(period)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start + 1.days()
        dateAdd.add(start, 2.days()) shouldBeEqualTo start + 2.days()
        dateAdd.add(start, 3.days()) shouldBeEqualTo period.end
        dateAdd.add(start, 3.days() + 1.nanos()) shouldBeEqualTo period.end + 1.nanos()
        dateAdd.add(start, 5.days()) shouldBeEqualTo period.end + 2.days()
    }

    @Test
    fun `two exclude periods`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)

        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 15),
            zonedDateTimeOf(2011, 4, 20)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 4, 22),
            zonedDateTimeOf(2011, 4, 25)
        )

        val dateAdd = CalendarDateAdd().apply {
            excludePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start + 1.days()
        dateAdd.add(start, 2.days()) shouldBeEqualTo start + 2.days()
        dateAdd.add(start, 3.days()) shouldBeEqualTo period1.end
        dateAdd.add(start, 4.days()) shouldBeEqualTo period1.end + 1.days()
        dateAdd.add(start, 5.days()) shouldBeEqualTo period2.end
        dateAdd.add(start, 6.days()) shouldBeEqualTo period2.end + 1.days()
        dateAdd.add(start, 7.days()) shouldBeEqualTo period2.end + 2.days()
    }

    @Test
    fun `when seek boundary mode`() = runTest {
        val dateAdd = CalendarDateAdd().apply {
            addWorkingWeekdays()
            excludePeriods.add(DayRange(2011, 4, 4, calendar))
            workingHours.add(HourRangeInDay(8, 18))
        }

        val start = zonedDateTimeOf(2011, 4, 1, 9, 0)

        dateAdd.add(start, 29.hours(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 4, 6, 18)
        dateAdd.add(start, 29.hours(), SeekBoundaryMode.NEXT) shouldBeEqualTo zonedDateTimeOf(2011, 4, 7, 8)
        dateAdd.add(start, 29.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 7, 8)
    }

    @Test
    fun `calendar date add 1`() = runTest {
        val dateAdd = CalendarDateAdd().apply {
            addWorkingWeekdays()
            excludePeriods.add(DayRange(zonedDateTimeOf(2011, 4, 4), calendar))
            workingHours.add(HourRangeInDay(8, 18))
        }

        val start = zonedDateTimeOf(2011, 4, 1, 9, 0)

        dateAdd.add(start, 22.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 6, 11)
        dateAdd.add(start, 22.hours(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 4, 6, 11)

        dateAdd.add(start, 29.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 7, 8)
        dateAdd.add(start, 29.hours(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 4, 6, 18)
    }

    @Test
    fun `calendar date add 2`() = runTest {
        val dateAdd = CalendarDateAdd().apply {
            addWorkingWeekdays()
            excludePeriods.add(DayRange(zonedDateTimeOf(2011, 4, 4), calendar))
            workingHours.add(HourRangeInDay(8, 12))
            workingHours.add(HourRangeInDay(13, 18))
        }

        val start = zonedDateTimeOf(2011, 4, 1, 9, 0)

        dateAdd.add(start, 3.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 1, 13)
        dateAdd.add(start, 4.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 1, 14)
        dateAdd.add(start, 8.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 5, 8)
    }

    @Test
    fun `calendar date add 3`() = runTest {
        val dateAdd = CalendarDateAdd().apply {
            addWorkingWeekdays()
            excludePeriods.add(DayRange(2011, 4, 4, calendar))
            workingHours.add(HourRangeInDay(LocalTime.of(8, 30), LocalTime.NOON))
            workingHours.add(HourRangeInDay(LocalTime.of(13, 30), LocalTime.of(18, 0)))
        }

        val start = zonedDateTimeOf(2011, 4, 1, 9, 0)

        dateAdd.add(start, 3.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 1, 13, 30)
        dateAdd.add(start, 4.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 1, 14, 30)
        dateAdd.add(start, 8.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 5, 9)
    }

    @Test
    fun `empty start week`() = runTest {
        val dateAdd = CalendarDateAdd()

        // 주중(월-금)을 working time으로 추가
        dateAdd.addWorkingWeekdays()

        val start = zonedDateTimeOf(2011, 4, 2, 13)

        // 4월 2일(토), 4월 3일(일)을 제외하면 4월 4일 0시부터 20시간
        dateAdd.add(start, 20.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 4, 20)

        // 4월 2일(토), 4월 3일(일) 제외하면 4월 4일 0시부터 24시간
        dateAdd.add(start, 24.hours()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 5)

        // 4월 2일(토), 4월 3일(일) 제외하면, 4월 4일부터 5일이면 주말인 4월 9일(토), 4월 10일(일) 제외한 4월 11일!!!
        dateAdd.add(start, 5.days()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 11)
    }
}
