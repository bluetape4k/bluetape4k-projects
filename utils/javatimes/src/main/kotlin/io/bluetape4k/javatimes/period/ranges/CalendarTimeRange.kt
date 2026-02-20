package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.period.assertValidPeriod
import io.bluetape4k.javatimes.startOfDay
import io.bluetape4k.javatimes.startOfHour
import io.bluetape4k.javatimes.startOfMinute
import io.bluetape4k.javatimes.startOfMonth
import io.bluetape4k.javatimes.startOfSecond
import io.bluetape4k.javatimes.startOfYear
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.hashOf
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime

/**
 * [calendar]가 적용된 기간을 나타내는 클래스입니다.
 */
open class CalendarTimeRange protected constructor(
    val period: ITimePeriod,
    val calendar: ITimeCalendar,
): TimeRange(calendar.mapStart(period.start), calendar.mapEnd(period.end), true) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            period: ITimePeriod = TimeRange.AnyTime,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): CalendarTimeRange {
            return CalendarTimeRange(period, calendar)
        }

        @JvmStatic
        operator fun invoke(
            start: ZonedDateTime,
            end: ZonedDateTime,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): CalendarTimeRange {
            return CalendarTimeRange(TimeRange(start, end), calendar)
        }
    }

    init {
        assertValidPeriod(calendar.mapStart(period.start), calendar.mapEnd(period.end))
    }

    override fun copy(offset: Duration): CalendarTimeRange =
        CalendarTimeRange(period.copy(offset), calendar)

    val startYear: Int get() = start.year
    val startMonthOfYear: Int get() = start.monthValue
    val startDayOfMonth: Int get() = start.dayOfMonth
    val startDayOfYear: Int get() = start.dayOfYear
    val startDayOfWeek: DayOfWeek get() = start.dayOfWeek
    val startHourOfDay: Int get() = start.hour
    val startMinuteOfHour: Int get() = start.minute
    val startSecondOfMinute: Int get() = start.second

    val endYear: Int get() = end.year
    val endMonthOfYear: Int get() = end.monthValue
    val endDayOfMonth: Int get() = end.dayOfMonth
    val endDayOfYear: Int get() = end.dayOfYear
    val endDayOfWeek: DayOfWeek get() = end.dayOfWeek
    val endHourOfDay: Int get() = end.hour
    val endMinuteOfHour: Int get() = end.minute
    val endSecondOfMinute: Int get() = end.second

    val mappedStart: ZonedDateTime get() = start
    val mappedEnd: ZonedDateTime get() = end
    val unmappedStart: ZonedDateTime get() = calendar.unmapStart(start)
    val unmappedEnd: ZonedDateTime get() = calendar.unmapEnd(end)

    val startYearOfStart: ZonedDateTime get() = start.startOfYear()
    val startMonthOfStart: ZonedDateTime get() = start.startOfMonth()
    val startDayOfStart: ZonedDateTime get() = start.startOfDay()
    val startHourOfStart: ZonedDateTime get() = start.startOfHour()
    val startMinuteOfStart: ZonedDateTime get() = start.startOfMinute()
    val startSecondOfStart: ZonedDateTime get() = start.startOfSecond()

    val startYearOfEnd: ZonedDateTime get() = end.startOfYear()
    val startMonthOfEnd: ZonedDateTime get() = end.startOfMonth()
    val startDayOfEnd: ZonedDateTime get() = end.startOfDay()
    val startHourOfEnd: ZonedDateTime get() = end.startOfHour()
    val startMinuteOfEnd: ZonedDateTime get() = end.startOfMinute()
    val startSecondOfEnd: ZonedDateTime get() = end.startOfSecond()


    override fun equalProperties(other: Any): Boolean {
        return super.equalProperties(other) &&
                other is CalendarTimeRange &&
                calendar == other.calendar
    }

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = hashOf(super.hashCode(), calendar)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("period", period)
            .add("calendar", calendar)
    }
}
