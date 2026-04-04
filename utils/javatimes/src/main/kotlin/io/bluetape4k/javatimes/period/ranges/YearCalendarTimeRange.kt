package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimePeriod
import io.bluetape4k.javatimes.period.yearOf

/**
 * 연(Year) 범위를 나타내는 클래스
 *
 * ```kotlin
 * val range = YearCalendarTimeRange(TimePeriod.AnyTime)
 * range.baseYear // 달력 기준의 연도
 * range.startYear
 * range.endYear
 * ```
 *
 * @param period 기간
 * @param calendar 달력
 */
open class YearCalendarTimeRange(
    period: ITimePeriod = TimePeriod.AnyTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(period, calendar) {

    /**
     * [calendar]의 기준 월을 적용했을 때의 기준 연도입니다.
     */
    val baseYear: Int get() = yearOf(startYear, startMonthOfYear, calendar)
}
