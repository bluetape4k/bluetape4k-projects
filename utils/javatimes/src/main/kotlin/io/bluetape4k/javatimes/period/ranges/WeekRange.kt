package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import java.time.ZonedDateTime

/**
 * 한 주(week) 를 범위로 나타내는 클래스입니다.
 *
 * ```kotlin
 * val week = WeekRange(2024, 3) // 2024년 3번째 주
 * val next = week.nextWeek()    // 2024년 4번째 주
 * val prev = week.prevWeek()    // 2024년 2번째 주
 * ```
 */
open class WeekRange(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): WeekTimeRange(startTime, 1, calendar) {

    constructor(
        weekyear: Int,
        weekOfWeekyear: Int,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(calendar.startOfYearWeek(weekyear, weekOfWeekyear), calendar)

    val firstDayOfWeek: ZonedDateTime get() = start
    val lastDayOfWeek: ZonedDateTime get() = end

    /**
     * 이 주가 두 개의 캘린더 연도에 걸쳐 있는지 여부를 반환합니다.
     *
     * ```kotlin
     * val week = WeekRange(ZonedDateTime.of(2024, 12, 30, 0, 0, 0, 0, ZoneOffset.UTC))
     * week.isMultipleCalendarYears() // true (12월 말 ~ 1월 초)
     * ```
     */
    fun isMultipleCalendarYears(): Boolean = calendar.year(firstDayOfWeek) != calendar.year(lastDayOfWeek)

    /**
     * 현재 주에서 [weeks]만큼 더한 [WeekRange]를 반환합니다.
     *
     * ```kotlin
     * val week = WeekRange(2024, 1)
     * val fourWeeksLater = week.addWeeks(4) // 4주 뒤
     * ```
     */
    fun addWeeks(weeks: Int): WeekRange = WeekRange(start.plusWeeks(weeks.toLong()), calendar)

    /**
     * 현재 주에서 [weeks]만큼 더한 [WeekRange]를 반환합니다.
     *
     * ```kotlin
     * val week = WeekRange(2024, 1)
     * val threeWeeksLater = week.addWeeks(3L) // 3주 뒤
     * ```
     */
    fun addWeeks(weeks: Long): WeekRange = WeekRange(start.plusWeeks(weeks), calendar)

    /**
     * 이전 주 [WeekRange]를 반환합니다.
     *
     * ```kotlin
     * val week3 = WeekRange(2024, 3)
     * val week2 = week3.prevWeek() // 2024년 2주차
     * ```
     */
    fun prevWeek(): WeekRange = addWeeks(-1)

    /**
     * 다음 주 [WeekRange]를 반환합니다.
     *
     * ```kotlin
     * val week3 = WeekRange(2024, 3)
     * val week4 = week3.nextWeek() // 2024년 4주차
     * ```
     */
    fun nextWeek(): WeekRange = addWeeks(1)
}
