package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.MonthsPerYear
import io.bluetape4k.javatimes.QuartersPerYear
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeYearPeriodOf

/**
 * [year]를 시작으로 [yearCount]만큼의 기간을 나타내는 클래스입니다.
 *
 * ```
 * val yearRange = YearTimeRange(2022, 3)  // 2022, 2023, 2024
 * ```
 *
 * @param year 시작 연도
 * @param yearCount 연도 개수
 * @param calendar 시간 달력
 * @see io.bluetape4k.javatimes.period.ranges.YearCalendarTimeRange
 */
open class YearTimeRange(
    val year: Int,
    val yearCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): YearCalendarTimeRange(relativeYearPeriodOf(year, yearCount), calendar) {

    fun quarterSequence(): Sequence<QuarterRange> =
        quarterRanges(start, yearCount * QuartersPerYear, calendar)

    fun quarters(): List<QuarterRange> = quarterSequence().toFastList()

    fun monthSequence(): Sequence<MonthRange> =
        monthRanges(start, yearCount * MonthsPerYear, calendar)

    fun months(): List<MonthRange> = monthSequence().toFastList()

    fun daySequence(): Sequence<DayRange> = monthSequence().flatMap { it.daySequence() }

    fun days(): List<DayRange> = daySequence().toFastList()

    fun hourSequence(): Sequence<HourRange> = daySequence().flatMap { it.hourSequence() }

    fun hours(): List<HourRange> = hourSequence().toFastList()

    fun minuteSequence(): Sequence<MinuteRange> = hourSequence().flatMap { it.minuteSequence() }

    fun minutes(): List<MinuteRange> = minuteSequence().toFastList()
}
