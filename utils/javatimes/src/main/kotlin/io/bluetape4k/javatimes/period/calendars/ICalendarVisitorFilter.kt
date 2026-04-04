package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.ValueObject
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import java.time.DayOfWeek

/**
 * Calendar 탐색 시의 필터링을 할 조건을 표현하는 인터페이스입니다.
 *
 * ```kotlin
 * val filter: ICalendarVisitorFilter = CalendarVisitorFilter()
 * filter.addWorkingWeekdays() // 월~금 요일만 포함
 * filter.addWorkingWeekends() // 토~일 요일만 포함
 * filter.addYears(2024)       // 2024년만 포함
 * filter.dayOfWeeks           // 설정된 요일 집합
 * ```
 */
interface ICalendarVisitorFilter: ValueObject {

    val excludePeriods: ITimePeriodCollection

    val years: MutableList<Int>

    val monthOfYears: MutableList<Int>

    val dayOfMonths: MutableList<Int>

    val dayOfWeeks: MutableSet<DayOfWeek>

    val hourOfDays: MutableList<Int>

    val minuteOfHours: MutableList<Int>

    fun addWorkingWeekdays()

    fun addWorkingWeekends()

    fun addDayOfWeeks(dayOfWeeks: Set<DayOfWeek>)

    fun clear()

}
