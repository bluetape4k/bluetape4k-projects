package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.DaysPerWeek
import io.bluetape4k.javatimes.MonthsPerYear
import io.bluetape4k.javatimes.Quarter
import io.bluetape4k.javatimes.QuartersPerYear
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.isLeapYear
import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.nanoOfDay
import io.bluetape4k.javatimes.nanos
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.yearOf
import io.bluetape4k.javatimes.startOfWeek
import io.bluetape4k.javatimes.yearPeriod
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.hashOf
import java.time.Duration
import java.time.Month
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

open class DateDiff private constructor(
    val start: ZonedDateTime,
    val end: ZonedDateTime = ZonedDateTime.now(),
    val calendar: ITimeCalendar = TimeCalendar.Default,
): AbstractValueObject() {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            start: ZonedDateTime,
            end: ZonedDateTime = ZonedDateTime.now(),
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): DateDiff {
            return DateDiff(start, end, calendar)
        }
    }

    val difference: Duration = Duration.between(start, end)
    val isEmpty: Boolean = difference.isZero

    val startYear: Int get() = calendar.year(start)
    val endYear: Int get() = calendar.year(end)

    val startMonthOfYear: Int get() = calendar.monthOfYear(start)
    val endMonthOfYear: Int get() = calendar.monthOfYear(end)

    val startMonth: Month get() = Month.of(startMonthOfYear)
    val endMonth: Month get() = Month.of(endMonthOfYear)

    val years: Long by lazy { calcYears() }
    val quarters: Long by lazy { calcQuarters() }
    val months: Long by lazy { calcMonths() }
    val weeks: Long by lazy { calcWeeks() }
    val days: Long get() = difference.toDays()
    val hours: Long get() = difference.toHours()
    val minutes: Long get() = difference.toMinutes()
    val seconds: Long get() = difference.seconds

    val elapsedYears: Long get() = years
    val elapsedQuarters: Long get() = quarters
    val elapsedMonths: Long get() = months - elapsedYears * MonthsPerYear

    val elapsedStartDay: ZonedDateTime
        get() = start.plusYears(elapsedYears).plusMonths(elapsedMonths)

    val elapsedDays: Long get() = Duration.between(elapsedStartDay, end).toDays()

    val elapsedStartHour: ZonedDateTime get() = elapsedStartDay + elapsedDays.days()
    val elapsedHours: Long get() = Duration.between(elapsedStartHour, end).toHours()

    val elapsedStartMinute: ZonedDateTime get() = elapsedStartHour + elapsedHours.hours()
    val elapsedMinutes: Long get() = Duration.between(elapsedStartMinute, end).toMinutes()

    val elapsedStartSecond: ZonedDateTime get() = elapsedStartMinute + elapsedMinutes.minutes()
    val elapsedSeconds: Long get() = Duration.between(elapsedStartSecond, end).seconds


    private fun calcYears(): Long {
        log.trace { "Calc difference by year ... " }

        if (isEmpty) return 0L

        val compareDay = minOf(end.dayOfMonth, calendar.daysInMonth(startYear, endMonthOfYear))
        var compareDate = zonedDateTimeOf(startYear, endMonthOfYear, compareDay) + end.nanoOfDay.nanos()

        if (end > start) {
            if (!start.year.isLeapYear()) {
                if (compareDate < start) {
                    compareDate += 1.yearPeriod()
                }
            } else {
                if (compareDate < start.minusDays(1)) {
                    compareDate += 1.yearPeriod()
                }
            }
        } else if (compareDate > start) {
            compareDate -= 1.yearPeriod()
        }

        val diff = (endYear - calendar.year(compareDate).toLong())
        log.trace { "Calc difference by year = $diff, compareDate=$compareDate" }
        return diff
    }

    private fun calcQuarters(): Long {
        log.trace { "Calc difference by quarter ... " }

        if (isEmpty) return 0L

        val y1: Int = yearOf(startYear, startMonthOfYear, calendar)
        val q1: Int = Quarter.ofMonth(startMonthOfYear).number

        val y2: Int = yearOf(endYear, endMonthOfYear, calendar)
        val q2: Int = Quarter.ofMonth(endMonthOfYear).number

        val diff: Int = (y2 * QuartersPerYear + q2) - (y1 * QuartersPerYear + q1)

        log.trace { "Calc difference by quarter. diff=$diff, y1=$y1, q1=$q1, y2=$y2, q2=$q2" }
        return diff.toLong()
    }

    private fun calcMonths(): Long {
        log.trace { "Calc difference by month ... " }

        if (isEmpty) return 0L

        val compareDay = minOf(end.dayOfMonth, calendar.daysInMonth(startYear, startMonthOfYear))
        var compareDate = zonedDateTimeOf(startYear, startMonthOfYear, compareDay).plusNanos(end.nanoOfDay)

        if (end > start) {
            if (!start.year.isLeapYear()) {
                if (compareDate < start) {
                    compareDate += 1.monthPeriod()
                }
            } else if (compareDate < start.minusDays(1)) {
                compareDate += 1.monthPeriod()
            }
        } else if (compareDate > start) {
            compareDate -= 1.monthPeriod()
        }

        val diff = (endYear * MonthsPerYear + endMonthOfYear) -
                (calendar.year(compareDate) * MonthsPerYear + calendar.monthOfYear(compareDate))

        log.trace { "Calc difference by month = $diff" }
        return diff.toLong()
    }

    private fun calcWeeks(): Long {
        log.trace { "Calc difference by week ... " }

        val w1 = start.startOfWeek()
        val w2 = end.startOfWeek()

        val diff = if (w1 == w2) 0L else Duration.between(w1, w2).toDays() / DaysPerWeek

        log.trace { "Calc difference by week = $diff" }
        return diff
    }

    private fun roundEx(n: Double): Double {
        val rounded = n.absoluteValue.roundToLong()
        return rounded.toDouble()
    }

    override fun equalProperties(other: Any): Boolean =
        other is DateDiff &&
                start == other.start &&
                end == other.end &&
                difference == other.difference &&
                calendar == other.calendar

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = hashOf(start, end, calendar)

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("start", start)
            .add("end", end)
            .add("differnce", difference)
            .add("calendar", calendar)
}
