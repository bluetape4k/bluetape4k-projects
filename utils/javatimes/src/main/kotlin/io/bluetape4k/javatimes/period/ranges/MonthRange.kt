package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.YearMonth
import java.time.ZonedDateTime

/**
 * 1개월 범위를 나타내는 클래스입니다.
 *
 * ```
 * val monthRange = MonthRange(ZonedDateTime.now())  // 오늘이 포함된 월의 시작 ~ 끝
 * ```
 */
open class MonthRange(
    startTime: ZonedDateTime = todayZonedDateTime(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): MonthTimeRange(startTime, 1, calendar) {

    constructor(year: Int, monthOfYear: Int, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(year, monthOfYear), calendar)

    constructor(yearMonth: YearMonth, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(yearMonth.year, yearMonth.monthValue), calendar)

    val year: Int get() = startYear
    val monthOfYear: Int get() = startMonthOfYear

    fun addMonths(months: Int): MonthRange = MonthRange(start.plusMonths(months.toLong()), calendar)

    fun prevMonth(): MonthRange = addMonths(-1)
    fun nextMonth(): MonthRange = addMonths(1)
}
