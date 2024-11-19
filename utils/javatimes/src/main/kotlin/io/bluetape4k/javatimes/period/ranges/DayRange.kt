package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * 하루를 나타내는 클래스입니다.
 */
open class DayRange(
    startTime: ZonedDateTime = todayZonedDateTime(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): DayTimeRange(startTime, 1, calendar) {

    constructor(year: Int, monthOfYear: Int, dayOfMonth: Int = 1, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(year, monthOfYear, dayOfMonth), calendar)

    val year: Int get() = startYear
    val monthOfYear: Int get() = startMonthOfYear
    val dayOfMonth: Int get() = startDayOfMonth
    val dayOfWeek: DayOfWeek get() = startDayOfWeek

    fun addDays(days: Int): DayRange = DayRange(start.plusDays(days.toLong()), calendar)

    fun prevDay(): DayRange = addDays(-1)
    fun nextDay(): DayRange = addDays(1)
}
