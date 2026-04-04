package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.Weekdays
import io.bluetape4k.javatimes.Weekends
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.TimePeriodCollection
import io.bluetape4k.support.hashOf
import java.io.Serializable
import java.time.DayOfWeek
import java.time.Month

/**
 * Calendar 탐색 시의 필터링을 할 조건을 가지는 클래스입니다.
 *
 * ```kotlin
 * val filter = CalendarVisitorFilter()
 * filter.addWorkingWeekdays() // 월~금 요일만 포함
 * filter.addYears(2024)       // 2024년만 필터링
 * filter.addMonthOfYears(Month.JANUARY, Month.FEBRUARY) // 1월, 2월만 수집
 * filter.dayOfWeeks // [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
 * ```
 */
open class CalendarVisitorFilter: AbstractValueObject(), ICalendarVisitorFilter, Serializable {

    override val excludePeriods: ITimePeriodCollection = TimePeriodCollection()

    override val years: MutableList<Int> = mutableListOf()

    override val monthOfYears: MutableList<Int> = mutableListOf()

    override val dayOfMonths: MutableList<Int> = mutableListOf()

    override val dayOfWeeks: MutableSet<DayOfWeek> = mutableSetOf()

    override val hourOfDays: MutableList<Int> = mutableListOf()

    override val minuteOfHours: MutableList<Int> = mutableListOf()

    fun addYears(vararg years: Int) {
        this.years.addAll(years.asList())
    }

    fun addMonthOfYears(vararg months: Month) {
        months.forEach { monthOfYears.add(it.value) }
    }

    fun addMonthOfYears(vararg monthOfYears: Int) {
        this.monthOfYears.addAll(monthOfYears.asList())
    }

    fun addDayOfMonths(vararg days: Int) {
        this.dayOfMonths.addAll(days.asList())
    }

    fun addDayOfWeeks(vararg dows: DayOfWeek) {
        this.dayOfWeeks.addAll(dows)
    }

    fun addHourOfDays(vararg hourOfDays: Int) {
        this.hourOfDays.addAll(hourOfDays.asList())
    }

    fun addMinuteOfHours(vararg minuteOfHours: Int) {
        this.minuteOfHours.addAll(minuteOfHours.asList())
    }

    override fun addWorkingWeekdays() {
        addDayOfWeeks(Weekdays.toSet())
    }

    override fun addWorkingWeekends() {
        addDayOfWeeks(Weekends.toSet())
    }

    override fun addDayOfWeeks(dayOfWeeks: Set<DayOfWeek>) {
        this.dayOfWeeks.addAll(dayOfWeeks)
    }

    override fun clear() {
        years.clear()
        monthOfYears.clear()
        dayOfMonths.clear()
        dayOfWeeks.clear()
        hourOfDays.clear()
        minuteOfHours.clear()
    }

    override fun equalProperties(other: Any): Boolean {
        return other is CalendarVisitorFilter &&
                years == other.years &&
                monthOfYears == other.monthOfYears &&
                dayOfMonths == other.dayOfMonths &&
                minuteOfHours == other.minuteOfHours &&
                dayOfWeeks == other.dayOfWeeks &&
                excludePeriods == other.excludePeriods
    }

    override fun hashCode(): Int =
        hashOf(years, monthOfYears, dayOfMonths, hourOfDays, minuteOfHours, dayOfWeeks, excludePeriods)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("years", years)
            .add("monthOfYears", monthOfYears)
            .add("dayOfMonths", dayOfMonths)
            .add("hourOfDays", hourOfDays)
            .add("minuteOfHours", minuteOfHours)
            .add("dayOfWeeks", dayOfWeeks)
            .add("excludePeriods", excludePeriods)
    }
}
