package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.YearMonth
import java.time.ZoneId
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

    /**
     * [year], [monthOfYear] 기준의 [MonthRange]를 생성합니다. 기본 zone은 UTC입니다.
     */
    constructor(year: Int, monthOfYear: Int, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(year, monthOfYear), calendar)

    constructor(year: Int, monthOfYear: Int, zoneId: ZoneId, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(year, monthOfYear, zoneId = zoneId), calendar)

    constructor(yearMonth: YearMonth, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(yearMonth.year, yearMonth.monthValue), calendar)

    constructor(yearMonth: YearMonth, zoneId: ZoneId, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(yearMonth.year, yearMonth.monthValue, zoneId = zoneId), calendar)

    val year: Int get() = startYear
    val monthOfYear: Int get() = startMonthOfYear

    /**
     * 현재 월에서 [months]만큼 더한 [MonthRange]를 반환합니다.
     *
     * ```kotlin
     * val jan2024 = MonthRange(2024, 1)
     * val apr2024 = jan2024.addMonths(3) // 2024년 4월
     * ```
     */
    fun addMonths(months: Int): MonthRange = MonthRange(start.plusMonths(months.toLong()), calendar)

    /**
     * 이전 월 [MonthRange]를 반환합니다.
     *
     * ```kotlin
     * val feb2024 = MonthRange(2024, 2)
     * val jan2024 = feb2024.prevMonth() // 2024년 1월
     * ```
     */
    fun prevMonth(): MonthRange = addMonths(-1)

    /**
     * 다음 월 [MonthRange]를 반환합니다.
     *
     * ```kotlin
     * val jan2024 = MonthRange(2024, 1)
     * val feb2024 = jan2024.nextMonth() // 2024년 2월
     * ```
     */
    fun nextMonth(): MonthRange = addMonths(1)
}
