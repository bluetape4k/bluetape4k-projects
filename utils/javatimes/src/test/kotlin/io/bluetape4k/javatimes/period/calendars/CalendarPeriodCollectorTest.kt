package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.Weekdays
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.ranges.CalendarTimeRange
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.javatimes.period.ranges.HourRangeCollection
import io.bluetape4k.javatimes.period.ranges.HourRangeInDay
import io.bluetape4k.javatimes.period.ranges.MonthRange
import io.bluetape4k.javatimes.period.ranges.WeekRangeCollection
import io.bluetape4k.javatimes.period.ranges.YearRange
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.Month

class CalendarPeriodCollectorTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `collect years`() = runTest {
        val filter = CalendarPeriodCollectorFilter().apply {
            years.addAll(listOf(2006, 2007, 2012))
        }

        val limits = CalendarTimeRange(
            zonedDateTimeOf(2001, 1, 1),
            zonedDateTimeOf(2019, 12, 31)
        )
        val collector = CalendarPeriodCollector(filter, limits)

        collector.collectYears()

        log.trace { "Collect years ... periods=${collector.periods}" }

        collector.periods.forEachIndexed { index, period ->
            period.isSamePeriod(YearRange(filter.years[index])).shouldBeTrue()
        }
    }

    @Test
    fun `collect months`() = runTest {
        val filter = CalendarPeriodCollectorFilter().apply {
            monthOfYears.add(Month.JANUARY.value)
        }

        val limits = CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 1),
            zonedDateTimeOf(2011, 12, 31)
        )

        val collector = CalendarPeriodCollector(filter, limits)
        collector.collectMonths()

        log.trace { "Collect months... periods=${collector.periods}" }

        collector.periods shouldHaveSize 2
        collector.periods[0] shouldBeEqualTo MonthRange(2010, 1)
        collector.periods[1] shouldBeEqualTo MonthRange(2011, 1)
    }

    @Test
    fun `collect days`() = runTest {
        val filter = CalendarPeriodCollectorFilter().apply {
            // 1월의 금요일만 추출
            monthOfYears.add(Month.JANUARY.value)
            dayOfWeeks += DayOfWeek.FRIDAY
        }

        val limits = CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 1),
            zonedDateTimeOf(2011, 12, 31)
        )
        val collector = CalendarPeriodCollector(filter, limits)

        collector.collectDays()
        log.trace { "collect days=${collector.periods}" }

        collector.periods shouldHaveSize 9

        collector.periods[0] shouldBeEqualTo DayRange(2010, 1, 1)
        collector.periods[1] shouldBeEqualTo DayRange(2010, 1, 8)
        collector.periods[2] shouldBeEqualTo DayRange(2010, 1, 15)
        collector.periods[3] shouldBeEqualTo DayRange(2010, 1, 22)
        collector.periods[4] shouldBeEqualTo DayRange(2010, 1, 29)

        collector.periods[5] shouldBeEqualTo DayRange(2011, 1, 7)
        collector.periods[6] shouldBeEqualTo DayRange(2011, 1, 14)
        collector.periods[7] shouldBeEqualTo DayRange(2011, 1, 21)
        collector.periods[8] shouldBeEqualTo DayRange(2011, 1, 28)
    }

    @Test
    fun `collect hours`() = runTest {
        val filter = CalendarPeriodCollectorFilter().apply {
            // 1월의 금요일의 08:00~18:00 추출
            monthOfYears.add(Month.JANUARY.value)
            dayOfWeeks += DayOfWeek.FRIDAY
            collectingHours.add(HourRangeInDay(8, 18))
        }

        val limits = CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 1),
            zonedDateTimeOf(2011, 12, 31)
        )
        val collector = CalendarPeriodCollector(filter, limits)

        collector.collectHours()

        collector.periods.forEach {
            log.trace { "hour period=$it" }
        }

        collector.periods shouldHaveSize 9

        collector.periods[0] shouldBeEqualTo HourRangeCollection(2010, 1, 1, 8, 10)
        collector.periods[1] shouldBeEqualTo HourRangeCollection(2010, 1, 8, 8, 10)
        collector.periods[2] shouldBeEqualTo HourRangeCollection(2010, 1, 15, 8, 10)
        collector.periods[3] shouldBeEqualTo HourRangeCollection(2010, 1, 22, 8, 10)
        collector.periods[4] shouldBeEqualTo HourRangeCollection(2010, 1, 29, 8, 10)

        collector.periods[5] shouldBeEqualTo HourRangeCollection(2011, 1, 7, 8, 10)
        collector.periods[6] shouldBeEqualTo HourRangeCollection(2011, 1, 14, 8, 10)
        collector.periods[7] shouldBeEqualTo HourRangeCollection(2011, 1, 21, 8, 10)
        collector.periods[8] shouldBeEqualTo HourRangeCollection(2011, 1, 28, 8, 10)
    }

    @Test
    fun `collect hours with minutes`() = runTest {
        val filter = CalendarPeriodCollectorFilter().apply {
            // 1월의 금요일의 08:00~18:00 추출
            monthOfYears.add(Month.JANUARY.value)
            dayOfWeeks += DayOfWeek.FRIDAY
            collectingHours.add(HourRangeInDay(LocalTime.of(8, 30), LocalTime.of(18, 50)))
        }

        val limits = CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 1),
            zonedDateTimeOf(2011, 12, 31)
        )
        val collector = CalendarPeriodCollector(filter, limits)

        collector.collectHours()
        collector.periods.forEach {
            log.trace { "hour period=$it" }
        }

        collector.periods shouldHaveSize 9

        collector.periods[0] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 1, 8, 30),
            zonedDateTimeOf(2010, 1, 1, 18, 50),
        )
        collector.periods[1] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 8, 8, 30),
            zonedDateTimeOf(2010, 1, 8, 18, 50),
        )
        collector.periods[2] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 15, 8, 30),
            zonedDateTimeOf(2010, 1, 15, 18, 50),
        )
        collector.periods[3] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 22, 8, 30),
            zonedDateTimeOf(2010, 1, 22, 18, 50),
        )
        collector.periods[4] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2010, 1, 29, 8, 30),
            zonedDateTimeOf(2010, 1, 29, 18, 50),
        )

        collector.periods[5] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2011, 1, 7, 8, 30),
            zonedDateTimeOf(2011, 1, 7, 18, 50),
        )
        collector.periods[6] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2011, 1, 14, 8, 30),
            zonedDateTimeOf(2011, 1, 14, 18, 50),
        )
        collector.periods[7] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2011, 1, 21, 8, 30),
            zonedDateTimeOf(2011, 1, 21, 18, 50),
        )
        collector.periods[8] shouldBeEqualTo CalendarTimeRange(
            zonedDateTimeOf(2011, 1, 28, 8, 30),
            zonedDateTimeOf(2011, 1, 28, 18, 50),
        )
    }

    @Test
    fun `collect exclude periods`() = runTest {
        val workingDays2011 = 365 - 2 - (51 * 2) - 1
        val workingDaysMarch2011 = 31 - 8  // total days - weekend days

        val year2011 = YearRange(2011)

        val filter1 = CalendarPeriodCollectorFilter().apply { addWorkingWeekdays() }

        val collector1 = CalendarPeriodCollector(filter1, year2011)
        collector1.collectDays()

        collector1.periods shouldHaveSize workingDays2011

        // 3월 제외 (23일 제외)
        val filter2 = CalendarPeriodCollectorFilter().apply {
            addWorkingWeekdays()
            excludePeriods.add(MonthRange(2011, 3))
        }

        val collector2 = CalendarPeriodCollector(filter2, year2011)
        collector2.collectDays()
        collector2.periods shouldHaveSize workingDays2011 - workingDaysMarch2011

        // 2011년 26주차 ~ 27주차 (여름 휴가)
        val filter3 = CalendarPeriodCollectorFilter().apply {
            addWorkingWeekdays()
            excludePeriods.add(MonthRange(2011, 3))
            excludePeriods.add(WeekRangeCollection(2011, 26, 2))
        }

        val collector3 = CalendarPeriodCollector(filter3, year2011)
        collector3.collectDays()

        collector3.periods shouldHaveSize workingDays2011 - workingDaysMarch2011 - 2 * Weekdays.size
    }
}
