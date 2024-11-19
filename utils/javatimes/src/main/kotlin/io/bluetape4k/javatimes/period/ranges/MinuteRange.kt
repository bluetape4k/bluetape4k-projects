package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import java.time.ZonedDateTime

/**
 * 분 단위의 범위를 나타내는 클래스입니다.
 *
 * ```
 * val minuteRange = MinuteRange(ZonedDateTime.now())
 * ```
 *
 * @param moment 시작 시각 (default: 현재 시각)
 * @param calendar 시간 단위를 계산하는데 사용할 [ITimeCalendar] (default: [TimeCalendar.Default])
 */
open class MinuteRange(
    moment: ZonedDateTime = ZonedDateTime.now(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): MinuteTimeRange(moment, 1, calendar) {

    val year: Int get() = startYear
    val monthOfYear: Int get() = startMonthOfYear
    val dayOfMonth: Int get() = startDayOfMonth
    val hourOfDay: Int get() = startHourOfDay
    val minuteOfHour: Int get() = startMinuteOfHour
    val secondOfMinute: Int get() = startSecondOfMinute

    fun addMinutes(increment: Int): MinuteRange {
        return MinuteRange(start + increment.minutes(), calendar)
    }

    fun prevMinute(): MinuteRange = addMinutes(-1)
    fun nextMinute(): MinuteRange = addMinutes(1)
}
