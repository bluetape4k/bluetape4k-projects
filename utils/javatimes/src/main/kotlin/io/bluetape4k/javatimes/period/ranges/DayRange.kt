package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * 하루를 나타내는 클래스입니다.
 *
 * ```kotlin
 * val day = DayRange(2024, 6, 15) // 2024년 6월 15일
 * val prev = day.prevDay() // 2024년 6월 14일
 * val next = day.nextDay() // 2024년 6월 16일
 * ```
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

    /**
     * 현재 날짜에서 [days]만큼 더한 [DayRange]를 반환합니다.
     *
     * ```kotlin
     * val june15 = DayRange(2024, 6, 15)
     * val june20 = june15.addDays(5) // 2024년 6월 20일
     * ```
     */
    fun addDays(days: Int): DayRange = DayRange(start.plusDays(days.toLong()), calendar)

    /**
     * 이전 날짜 [DayRange]를 반환합니다.
     *
     * ```kotlin
     * val june15 = DayRange(2024, 6, 15)
     * val june14 = june15.prevDay() // 2024년 6월 14일
     * ```
     */
    fun prevDay(): DayRange = addDays(-1)

    /**
     * 다음 날짜 [DayRange]를 반환합니다.
     *
     * ```kotlin
     * val june15 = DayRange(2024, 6, 15)
     * val june16 = june15.nextDay() // 2024년 6월 16일
     * ```
     */
    fun nextDay(): DayRange = addDays(1)
}
