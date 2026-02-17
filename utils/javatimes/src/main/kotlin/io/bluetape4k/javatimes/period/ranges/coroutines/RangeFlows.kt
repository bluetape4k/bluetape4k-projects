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

/**
 * 지정한 시작 시각부터 [yearCount]만큼의 [YearRange]를 [Flow]로 반환합니다.
 *
 * @param startTime 시작 시각
 * @param yearCount 생성할 연도 범위 개수
 * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
 * @return [YearRange]의 [Flow]
 */
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

/**
 * 지정한 시작 시각부터 [monthCount]만큼의 [MonthRange]를 [Flow]로 반환합니다.
 *
 * @param startTime 시작 시각
 * @param monthCount 생성할 월 범위 개수
 * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
 * @return [MonthRange]의 [Flow]
 */
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

/**
 * 지정한 시작 시각부터 [weekCount]만큼의 [WeekRange]를 [Flow]로 반환합니다.
 *
 * @param startTime 시작 시각
 * @param weekCount 생성할 주 범위 개수
 * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
 * @return [WeekRange]의 [Flow]
 */
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

/**
 * 지정한 시작 시각부터 [dayCount]만큼의 [DayRange]를 [Flow]로 반환합니다.
 *
 * @param startTime 시작 시각
 * @param dayCount 생성할 일 범위 개수
 * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
 * @return [DayRange]의 [Flow]
 */
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

/**
 * 지정한 시작 시각부터 [hourCount]만큼의 [HourRange]를 [Flow]로 반환합니다.
 *
 * @param startTime 시작 시각
 * @param hourCount 생성할 시간 범위 개수
 * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
 * @return [HourRange]의 [Flow]
 */
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

/**
 * 지정한 시작 시각부터 [minuteCount]만큼의 [MinuteRange]를 [Flow]로 반환합니다.
 *
 * @param startTime 시작 시각
 * @param minuteCount 생성할 분 범위 개수
 * @param calendar 사용할 달력 (기본값: TimeCalendar.Default)
 * @return [MinuteRange]의 [Flow]
 */
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
