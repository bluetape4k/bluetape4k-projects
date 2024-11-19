package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeMonthPeriod
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

/**
 * 월 단위로 [monthCount] 만큼의 기간을 표현하는 클래스입니다.
 */
open class MonthTimeRange @JvmOverloads constructor(
    startTime: ZonedDateTime = todayZonedDateTime(),
    val monthCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(startTime.relativeMonthPeriod(monthCount), calendar) {

    fun daySequence(): Sequence<DayRange> = dayRanges(start, end, calendar)

    fun days(): List<DayRange> = daySequence().toList()

    fun hourSequence(): Sequence<HourRange> =
        daySequence().flatMap { it.hourSequence() }

    fun hours(): List<HourRange> = hourSequence().toList()
}
