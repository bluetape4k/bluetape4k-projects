package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MonthsPerYear
import io.bluetape4k.javatimes.QuartersPerYear
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeYearPeriod
import io.bluetape4k.javatimes.startOfYear
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * [startTime]이 속한 연도의 시작을 기준으로 [yearCount]만큼의 기간을 나타내는 클래스입니다.
 *
 * ```
 * val yearRange = YearTimeRange(2022, 3)  // 2022, 2023, 2024
 * ```
 *
 * @param startTime 기준 시각 (해당 시각의 zone을 유지)
 * @param yearCount 연도 개수
 * @param calendar 시간 달력
 * @see io.bluetape4k.javatimes.period.ranges.YearCalendarTimeRange
 */
open class YearTimeRange(
    startTime: ZonedDateTime,
    val yearCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): YearCalendarTimeRange(startTime.relativeYearPeriod(yearCount), calendar) {

    constructor(
        year: Int,
        yearCount: Int = 1,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(startOfYear(year, ZoneOffset.UTC), yearCount, calendar)

    constructor(
        year: Int,
        yearCount: Int = 1,
        zoneId: ZoneId,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(startOfYear(year, zoneId), yearCount, calendar)

    val year: Int get() = startYear

    fun quarterSequence(): Sequence<QuarterRange> =
        quarterRanges(start, yearCount * QuartersPerYear, calendar)

    fun quarters(): List<QuarterRange> = quarterSequence().toList()

    fun monthSequence(): Sequence<MonthRange> =
        monthRanges(start, yearCount * MonthsPerYear, calendar)

    fun months(): List<MonthRange> = monthSequence().toList()

    fun daySequence(): Sequence<DayRange> = monthSequence().flatMap { it.daySequence() }

    fun days(): List<DayRange> = daySequence().toList()

    fun hourSequence(): Sequence<HourRange> = daySequence().flatMap { it.hourSequence() }

    fun hours(): List<HourRange> = hourSequence().toList()

    fun minuteSequence(): Sequence<MinuteRange> = hourSequence().flatMap { it.minuteSequence() }

    fun minutes(): List<MinuteRange> = minuteSequence().toList()
}
