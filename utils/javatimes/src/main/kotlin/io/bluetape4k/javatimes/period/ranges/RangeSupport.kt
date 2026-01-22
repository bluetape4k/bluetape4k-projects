package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.weeks
import io.bluetape4k.support.assertPositiveNumber
import java.time.ZonedDateTime

/**
 * [year]로부터 [yearCount] 만큼의 [YearRange] 시퀀스를 생성합니다.
 *
 * ```
 * val years = yearRanges(2021, 3) // 2021, 2022, 2023
 * ```
 */
fun yearRanges(
    year: Int,
    yearCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<YearRange> = sequence {
    yearCount.assertPositiveNumber("yearCount")
    var currentYear = year
    repeat(yearCount) {
        yield(YearRange(currentYear, calendar))
        currentYear++
    }
}

/**
 * [startTime]으로부터 [quarterCount] 만큼의 [QuarterRange] 시퀀스를 생성합니다.
 *
 * ```
 * val quarters = quarterRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun quarterRanges(
    startTime: ZonedDateTime,
    quarterCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<QuarterRange> = sequence {
    quarterCount.assertPositiveNumber("quarterCount")
    var current = QuarterRange(startTime, calendar)
    repeat(quarterCount) {
        yield(current)
        current = current.nextQuarter()
    }
}

/**
 * [startTime]으로부터 [monthCount] 만큼의 [MonthRange] 시퀀스를 생성합니다.
 *
 * ```
 * val months = monthRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun monthRanges(
    startTime: ZonedDateTime,
    monthCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MonthRange> {
    monthCount.assertPositiveNumber("monthCount")
    return monthRanges(startTime, startTime + monthCount.monthPeriod(), calendar)
}

/**
 * [start]으로부터 [end]까지의 [MonthRange] 시퀀스를 생성합니다.
 *
 * ```
 * val months = monthRanges(ZonedDateTime.now(), ZonedDateTime.now().plusMonths(3))
 * ```
 */
fun monthRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MonthRange> = sequence {
    var current = MonthRange(start, calendar)
    while (current.end <= end) {
        yield(current)
        current = current.nextMonth()
    }
}

/**
 * [start]로부터 [weekCount] 만큼의 [WeekRange] 시퀀스를 생성합니다.
 *
 * ```
 * val weeks = weekRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun weekRanges(
    start: ZonedDateTime,
    weekCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<WeekRange> {
    weekCount.assertPositiveNumber("weekCount")
    return weekRanges(start, start + weekCount.weeks(), calendar)
}

/**
 * [start]로부터 [end]까지의 [WeekRange] 시퀀스를 생성합니다.
 *
 * ```
 * val weeks = weekRanges(ZonedDateTime.now(), ZonedDateTime.now().plusWeeks(3))
 * ```
 */
fun weekRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<WeekRange> = sequence {
    var current = WeekRange(start, calendar)
    while (current.end <= end) {
        yield(current)
        current = current.nextWeek()
    }
}

/**
 * [start]로부터 [dayCount] 만큼의 [DayRange] 시퀀스를 생성합니다.
 *
 * ```
 * val days = dayRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun dayRanges(
    start: ZonedDateTime,
    dayCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<DayRange> {
    dayCount.assertPositiveNumber("dayCount")
    return dayRanges(start, start + dayCount.days(), calendar)
}

/**
 * [start]로부터 [end]까지의 [DayRange] 시퀀스를 생성합니다.
 *
 * ```
 * val days = dayRanges(ZonedDateTime.now(), ZonedDateTime.now().plusDays(3))
 * ```
 */
fun dayRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<DayRange> = sequence {
    var current = DayRange(start, calendar)
    while (current.end <= end) {
        yield(current)
        current = current.nextDay()
    }
}

/**
 * [start]로부터 [hourCount] 만큼의 [HourRange] 시퀀스를 생성합니다.
 *
 * ```
 * val hours = hourRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun hourRanges(
    start: ZonedDateTime,
    hourCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<HourRange> {
    hourCount.assertPositiveNumber("hourCount")
    return hourRanges(start, start + hourCount.hours(), calendar)
}

/**
 * [start]로부터 [end]까지의 [HourRange] 시퀀스를 생성합니다.
 *
 * ```
 * val hours = hourRanges(ZonedDateTime.now(), ZonedDateTime.now().plusHours(3))
 * ```
 */
fun hourRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<HourRange> = sequence {
    var current = HourRange(start, calendar)
    while (current.end <= end) {
        yield(current)
        current = current.nextHour()
    }
}

/**
 * [start]로부터 [minuteCount] 만큼의 [MinuteRange] 시퀀스를 생성합니다.
 *
 * ```
 * val minutes = minuteRanges(ZonedDateTime.now(), 3)
 * ```
 */
fun minuteRanges(
    start: ZonedDateTime,
    minuteCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MinuteRange> {
    minuteCount.assertPositiveNumber("minuteCount")
    return minuteRanges(start, start + minuteCount.minutes(), calendar)
}

/**
 * [start]로부터 [end]까지의 [MinuteRange] 시퀀스를 생성합니다.
 *
 * ```
 * val minutes = minuteRanges(ZonedDateTime.now(), ZonedDateTime.now().plusMinutes(3))
 * ```
 */
fun minuteRanges(
    start: ZonedDateTime,
    end: ZonedDateTime,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Sequence<MinuteRange> = sequence {
    var current = MinuteRange(start, calendar)
    while (current.end <= end) {
        yield(current)
        current = current.nextMinute()
    }
}
