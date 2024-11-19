package io.bluetape4k.javatimes.period.ranges.coroutines

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.javatimes.period.ranges.HourRange
import io.bluetape4k.javatimes.period.ranges.MinuteRange
import io.bluetape4k.javatimes.period.ranges.MonthRange
import io.bluetape4k.javatimes.period.ranges.WeekRange
import io.bluetape4k.javatimes.period.ranges.YearRange
import io.bluetape4k.support.assertPositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.ZonedDateTime

fun flowOfYearRange(
    startTime: ZonedDateTime,
    yearCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Flow<YearRange> = flow {
    yearCount.assertPositiveNumber("yearCount")

    val start = YearRange(startTime, calendar)
    var years = 0
    while (years < yearCount) {
        emit(start.addYears(years))
        years++
    }
}

fun flowOfMonthRange(
    startTime: ZonedDateTime,
    monthCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Flow<MonthRange> = flow {
    monthCount.assertPositiveNumber("monthCount")

    val start = MonthRange(startTime, calendar)
    var months = 0
    while (months < monthCount) {
        emit(start.addMonths(months))
        months++
    }
}

fun flowOfWeekRange(
    startTime: ZonedDateTime,
    weekCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Flow<WeekRange> = flow {
    weekCount.assertPositiveNumber("weekCount")

    val start = WeekRange(startTime, calendar)
    var weeks = 0
    while (weeks < weekCount) {
        emit(start.addWeeks(weeks))
        weeks++
    }
}

fun flowOfDayRange(
    startTime: ZonedDateTime,
    dayCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Flow<DayRange> = flow {
    dayCount.assertPositiveNumber("dayCount")

    val start = DayRange(startTime, calendar)
    var days = 0
    while (days < dayCount) {
        emit(start.addDays(days))
        days++
    }
}

fun flowOfHourRange(
    startTime: ZonedDateTime,
    hourCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Flow<HourRange> = flow {
    hourCount.assertPositiveNumber("hourCount")
    val start = HourRange(startTime, calendar)
    var hours = 0
    while (hours < hourCount) {
        emit(start.addHours(hours))
        hours++
    }
}

fun flowOfMinuteRange(
    startTime: ZonedDateTime,
    minuteCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): Flow<MinuteRange> = flow {
    minuteCount.assertPositiveNumber("minuteCount")

    val start = MinuteRange(startTime, calendar)
    var minutes = 0
    while (minutes < minuteCount) {
        emit(start.addMinutes(minutes))
        minutes++
    }
}
