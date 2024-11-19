package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimePeriod

/**
 * 연(Year) 범위를 나타내는 클래스
 *
 * @param period 기간
 * @param calendar 달력
 */
open class YearCalendarTimeRange(
    period: ITimePeriod = TimePeriod.AnyTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(period, calendar) {

    private val baseMonth: Int = 1

    val baseYear: Int get() = startYear
}
