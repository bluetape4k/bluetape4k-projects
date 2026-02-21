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

/**
 * 두 날짜/시각 간의 차이를 계산하는 클래스
 *
 * 년, 분기, 월, 주, 일, 시, 분, 초 단위로 두 시각의 차이를 계산합니다.
 *
 * @param start 시작 시각
 * @param end 종료 시각 (기본값: 현재 시각)
 * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
 */
open class DateDiff private constructor(
    val start: ZonedDateTime,
    val end: ZonedDateTime = ZonedDateTime.now(),
    val calendar: ITimeCalendar = TimeCalendar.Default,
): AbstractValueObject() {

    companion object: KLogging() {
        /**
         * [DateDiff] 인스턴스를 생성합니다.
         *
         * @param start 시작 시각
         * @param end 종료 시각 (기본값: 현재 시각)
         * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
         * @return 생성된 [DateDiff] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            start: ZonedDateTime,
            end: ZonedDateTime = ZonedDateTime.now(),
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): DateDiff {
            return DateDiff(start, end, calendar)
        }
    }

    /** 두 시각 사이의 Duration */
    val difference: Duration = Duration.between(start, end)

    /** 두 시각이 동일한지 여부 */
    val isEmpty: Boolean = difference.isZero

    /** 시작 시각의 연도 */
    val startYear: Int get() = calendar.year(start)

    /** 종료 시각의 연도 */
    val endYear: Int get() = calendar.year(end)

    /** 시작 시각의 월(1-12) */
    val startMonthOfYear: Int get() = calendar.monthOfYear(start)

    /** 종료 시각의 월(1-12) */
    val endMonthOfYear: Int get() = calendar.monthOfYear(end)

    /** 시작 시각의 월 */
    val startMonth: Month get() = Month.of(startMonthOfYear)

    /** 종료 시각의 월 */
    val endMonth: Month get() = Month.of(endMonthOfYear)

    /** 두 시각 사이의 연도 차이 */
    val years: Long by lazy { calcYears() }

    /** 두 시각 사이의 분기 차이 */
    val quarters: Long by lazy { calcQuarters() }

    /** 두 시각 사이의 월 차이 */
    val months: Long by lazy { calcMonths() }

    /** 두 시각 사이의 주 차이 */
    val weeks: Long by lazy { calcWeeks() }

    /** 두 시각 사이의 일 차이 */
    val days: Long get() = difference.toDays()

    /** 두 시각 사이의 시간 차이 */
    val hours: Long get() = difference.toHours()

    /** 두 시각 사이의 분 차이 */
    val minutes: Long get() = difference.toMinutes()

    /** 두 시각 사이의 초 차이 */
    val seconds: Long get() = difference.seconds

    /** 경과된 연도 */
    val elapsedYears: Long get() = years

    /** 경과된 분기 */
    val elapsedQuarters: Long get() = quarters

    /** 경과된 월 (연도를 제외한 나머지 월) */
    val elapsedMonths: Long get() = months - elapsedYears * MonthsPerYear

    /** 경과된 년/월을 더한 시작 시각 */
    val elapsedStartDay: ZonedDateTime
        get() = start.plusYears(elapsedYears).plusMonths(elapsedMonths)

    /** 경과된 일 (년/월을 제외한 나머지 일) */
    val elapsedDays: Long get() = Duration.between(elapsedStartDay, end).toDays()

    /** 경과된 년/월/일을 더한 시작 시각 */
    val elapsedStartHour: ZonedDateTime get() = elapsedStartDay + elapsedDays.days()

    /** 경과된 시간 (년/월/일을 제외한 나머지 시간) */
    val elapsedHours: Long get() = Duration.between(elapsedStartHour, end).toHours()

    /** 경과된 년/월/일/시를 더한 시작 시각 */
    val elapsedStartMinute: ZonedDateTime get() = elapsedStartHour + elapsedHours.hours()

    /** 경과된 분 (년/월/일/시를 제외한 나머지 분) */
    val elapsedMinutes: Long get() = Duration.between(elapsedStartMinute, end).toMinutes()

    /** 경과된 년/월/일/시/분을 더한 시작 시각 */
    val elapsedStartSecond: ZonedDateTime get() = elapsedStartMinute + elapsedMinutes.minutes()

    /** 경과된 초 (년/월/일/시/분을 제외한 나머지 초) */
    val elapsedSeconds: Long get() = Duration.between(elapsedStartSecond, end).seconds


    private fun calcYears(): Long {
        log.trace { "Calc difference by year ... " }

        if (isEmpty) return 0L

        val compareDay = minOf(end.dayOfMonth, calendar.daysInMonth(startYear, endMonthOfYear))
        var compareDate = zonedDateTimeOf(
            startYear,
            endMonthOfYear,
            compareDay,
            zoneId = start.zone,
        ) + end.nanoOfDay.nanos()

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
        var compareDate = zonedDateTimeOf(
            startYear,
            startMonthOfYear,
            compareDay,
            zoneId = start.zone,
        ).plusNanos(end.nanoOfDay)

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
            .add("difference", difference)
            .add("calendar", calendar)
}
