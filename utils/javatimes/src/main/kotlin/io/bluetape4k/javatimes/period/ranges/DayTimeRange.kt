package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.HoursPerDay
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeDayPeriod
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

/**
 * 일 단위로 [dayCount] 만큼의 기간을 표현하는 클래스입니다.
 *
 * ```kotlin
 * val now = ZonedDateTime.now()
 * val range = DayTimeRange(now, dayCount = 3)
 * range.start    // 오늘 자정
 * range.dayCount // 3
 * range.hours()  // 3일 x 24시간 = 72개의 HourRange
 * ```
 */
open class DayTimeRange(
    startTime: ZonedDateTime = todayZonedDateTime(),
    val dayCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(startTime.relativeDayPeriod(dayCount), calendar) {

    fun hourSequence(): Sequence<HourRange> =
        hourRanges(startDayOfStart, dayCount * HoursPerDay, calendar)

    fun hours(): List<HourRange> = hourSequence().toList()

    fun minuteSequence(): Sequence<MinuteRange> =
        hourSequence().flatMap { it.minuteSequence() }

    fun minutes(): List<MinuteRange> = minuteSequence().toList()
}
