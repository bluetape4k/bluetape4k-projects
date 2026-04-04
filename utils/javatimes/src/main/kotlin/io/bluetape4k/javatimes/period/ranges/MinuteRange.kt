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

    /**
     * 현재 분에서 [increment]만큼 더한 [MinuteRange]를 반환합니다.
     *
     * ```kotlin
     * val min30 = MinuteRange(ZonedDateTime.of(2024, 6, 15, 9, 30, 0, 0, ZoneOffset.UTC))
     * val min35 = min30.addMinutes(5) // 9시 35분
     * ```
     */
    fun addMinutes(increment: Int): MinuteRange {
        return MinuteRange(start + increment.minutes(), calendar)
    }

    /**
     * 이전 분 [MinuteRange]를 반환합니다.
     *
     * ```kotlin
     * val min30 = MinuteRange(ZonedDateTime.of(2024, 6, 15, 9, 30, 0, 0, ZoneOffset.UTC))
     * val min29 = min30.prevMinute() // 9시 29분
     * ```
     */
    fun prevMinute(): MinuteRange = addMinutes(-1)

    /**
     * 다음 분 [MinuteRange]를 반환합니다.
     *
     * ```kotlin
     * val min30 = MinuteRange(ZonedDateTime.of(2024, 6, 15, 9, 30, 0, 0, ZoneOffset.UTC))
     * val min31 = min30.nextMinute() // 9시 31분
     * ```
     */
    fun nextMinute(): MinuteRange = addMinutes(1)
}
