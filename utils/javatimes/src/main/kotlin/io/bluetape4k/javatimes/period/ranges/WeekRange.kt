package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfWeekOfWeekyear
import java.time.ZonedDateTime

/**
 * 한 주(week) 를 범위로 나타내는 클래스입니다.
 */
open class WeekRange(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): WeekTimeRange(startTime, 1, calendar) {

    constructor(weekyear: Int, weekOfWeekyear: Int, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(startOfWeekOfWeekyear(weekyear, weekOfWeekyear), calendar)

    val firstDayOfWeek: ZonedDateTime get() = start
    val lastDayOfWeek: ZonedDateTime get() = end

    fun isMultipleCalendarYears(): Boolean = calendar.year(firstDayOfWeek) != calendar.year(lastDayOfWeek)

    fun addWeeks(weeks: Int): WeekRange = WeekRange(start.plusWeeks(weeks.toLong()), calendar)
    fun addWeeks(weeks: Long): WeekRange = WeekRange(start.plusWeeks(weeks), calendar)

    fun prevWeek(): WeekRange = addWeeks(-1)
    fun nextWeek(): WeekRange = addWeeks(1)
}
