package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MonthsPerQuarter
import io.bluetape4k.javatimes.Quarter
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeQuarterPeriod
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

/**
 * 분기 단위로 [quarterCount] 만큼의 기간을 표현하는 클래스입니다.
 */
open class QuarterTimeRange(
    startTime: ZonedDateTime = todayZonedDateTime(),
    val quarterCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(startTime.relativeQuarterPeriod(quarterCount), calendar) {

    val quarterOfStart: Quarter get() = Quarter.ofMonth(startMonthOfYear)
    val quarterOfEnd: Quarter get() = Quarter.ofMonth(endMonthOfYear)

    val isMultipleCalendarYears: Boolean = startYear != endYear

    val isMultipleCalendarQuarters: Boolean = isMultipleCalendarYears || quarterOfStart != quarterOfEnd


    fun monthSequence(): Sequence<MonthRange> =
        monthRanges(startMonthOfStart, quarterCount * MonthsPerQuarter, calendar)

    fun months(): List<MonthRange> = monthSequence().toList()

    fun daySequence(): Sequence<DayRange> = monthSequence().flatMap { it.daySequence() }

    fun days(): List<DayRange> = daySequence().toList()
}
