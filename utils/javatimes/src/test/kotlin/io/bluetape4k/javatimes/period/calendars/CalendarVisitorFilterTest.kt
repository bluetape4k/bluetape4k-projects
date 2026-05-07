package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Month

class CalendarVisitorFilterTest {

    companion object : KLogging()

    @Test
    fun `default filter has all empty collections`() {
        val filter = CalendarVisitorFilter()
        filter.years.shouldBeEmpty()
        filter.monthOfYears.shouldBeEmpty()
        filter.dayOfMonths.shouldBeEmpty()
        filter.dayOfWeeks.shouldBeEmpty()
        filter.hourOfDays.shouldBeEmpty()
        filter.minuteOfHours.shouldBeEmpty()
    }

    @Test
    fun `addYears adds years to filter`() {
        val filter = CalendarVisitorFilter()
        filter.addYears(2023, 2024)
        filter.years shouldHaveSize 2
        filter.years.shouldContain(2023)
        filter.years.shouldContain(2024)
    }

    @Test
    fun `addMonthOfYears with Month enum values`() {
        val filter = CalendarVisitorFilter()
        filter.addMonthOfYears(Month.JANUARY, Month.FEBRUARY)
        filter.monthOfYears shouldHaveSize 2
        filter.monthOfYears.shouldContain(Month.JANUARY.value)
        filter.monthOfYears.shouldContain(Month.FEBRUARY.value)
    }

    @Test
    fun `addMonthOfYears with int values`() {
        val filter = CalendarVisitorFilter()
        filter.addMonthOfYears(3, 6, 9, 12)
        filter.monthOfYears shouldHaveSize 4
    }

    @Test
    fun `addDayOfMonths adds days`() {
        val filter = CalendarVisitorFilter()
        filter.addDayOfMonths(1, 15, 31)
        filter.dayOfMonths shouldHaveSize 3
        filter.dayOfMonths.shouldContain(1)
        filter.dayOfMonths.shouldContain(15)
    }

    @Test
    fun `addDayOfWeeks adds specific days`() {
        val filter = CalendarVisitorFilter()
        filter.addDayOfWeeks(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        filter.dayOfWeeks shouldHaveSize 3
        filter.dayOfWeeks.shouldContain(DayOfWeek.MONDAY)
        filter.dayOfWeeks.shouldContain(DayOfWeek.FRIDAY)
    }

    @Test
    fun `addWorkingWeekdays adds Monday to Friday`() {
        val filter = CalendarVisitorFilter()
        filter.addWorkingWeekdays()
        filter.dayOfWeeks shouldHaveSize 5
        filter.dayOfWeeks.shouldContain(DayOfWeek.MONDAY)
        filter.dayOfWeeks.shouldContain(DayOfWeek.TUESDAY)
        filter.dayOfWeeks.shouldContain(DayOfWeek.WEDNESDAY)
        filter.dayOfWeeks.shouldContain(DayOfWeek.THURSDAY)
        filter.dayOfWeeks.shouldContain(DayOfWeek.FRIDAY)
    }

    @Test
    fun `addWorkingWeekends adds Saturday and Sunday`() {
        val filter = CalendarVisitorFilter()
        filter.addWorkingWeekends()
        filter.dayOfWeeks shouldHaveSize 2
        filter.dayOfWeeks.shouldContain(DayOfWeek.SATURDAY)
        filter.dayOfWeeks.shouldContain(DayOfWeek.SUNDAY)
    }

    @Test
    fun `addHourOfDays adds business hours`() {
        val filter = CalendarVisitorFilter()
        filter.addHourOfDays(9, 10, 11, 12, 13, 14, 15, 16, 17)
        filter.hourOfDays shouldHaveSize 9
    }

    @Test
    fun `addMinuteOfHours adds specific minutes`() {
        val filter = CalendarVisitorFilter()
        filter.addMinuteOfHours(0, 15, 30, 45)
        filter.minuteOfHours shouldHaveSize 4
    }

    @Test
    fun `clear resets all filter fields`() {
        val filter = CalendarVisitorFilter()
        filter.addYears(2024)
        filter.addMonthOfYears(Month.JANUARY)
        filter.addDayOfMonths(1)
        filter.addDayOfWeeks(DayOfWeek.MONDAY)
        filter.addHourOfDays(9)
        filter.addMinuteOfHours(0)

        filter.clear()

        filter.years.shouldBeEmpty()
        filter.monthOfYears.shouldBeEmpty()
        filter.dayOfMonths.shouldBeEmpty()
        filter.dayOfWeeks.shouldBeEmpty()
        filter.hourOfDays.shouldBeEmpty()
        filter.minuteOfHours.shouldBeEmpty()
    }

    @Test
    fun `equals for two identical filters`() {
        val a = CalendarVisitorFilter()
        a.addYears(2024)
        a.addDayOfWeeks(DayOfWeek.MONDAY)

        val b = CalendarVisitorFilter()
        b.addYears(2024)
        b.addDayOfWeeks(DayOfWeek.MONDAY)

        a shouldBeEqualTo b
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = CalendarVisitorFilter()
        a.addYears(2024)

        val b = CalendarVisitorFilter()
        b.addYears(2024)

        a.hashCode() shouldBeEqualTo b.hashCode()
    }
}
