package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.DaysPerWeek
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeWeekPeriod
import io.bluetape4k.javatimes.weekOfWeekyear
import io.bluetape4k.javatimes.weekyear
import java.time.ZonedDateTime

/**
 * [weekCount] 만큼의 주를 기간으로 나타내는 클래스입니다.
 */
open class WeekTimeRange(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    val weekCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(startTime.relativeWeekPeriod(weekCount), calendar) {

    val year: Int get() = startYear
    val weekyear: Int get() = start.weekyear
    val weekOfWeekyear: Int get() = start.weekOfWeekyear

    val startWeekyear: Int get() = start.weekyear
    val startWeekOfWeekyear: Int get() = start.weekOfWeekyear
    val endWeekyear: Int get() = end.weekyear
    val endWeekOfWeekyear: Int get() = end.weekOfWeekyear

    fun daySequence(): Sequence<DayRange> =
        dayRanges(startDayOfStart, weekCount * DaysPerWeek, calendar)

    fun days(): List<DayRange> = daySequence().toList()
}
