package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfHour
import java.time.ZonedDateTime

/**
 * 시간 단위로 기간을 표현하는 클래스입니다.
 *
 * ```kotlin
 * val hour = HourRange(ZonedDateTime.of(2024, 6, 15, 9, 0, 0, 0, ZoneOffset.UTC))
 * hour.hourOfDay // 9
 * val nextHour = hour.nextHour() // 10시
 * ```
 */
open class HourRange(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): HourTimeRange(startTime, 1, calendar) {

    val year: Int get() = startYear
    val monthOfYear: Int get() = startMonthOfYear
    val dayOfMonth: Int get() = startDayOfMonth
    val hourOfDay: Int get() = startHourOfDay

    /**
     * 현재 시각에서 [hours]만큼 더한 [HourRange]를 반환합니다.
     *
     * ```kotlin
     * val hour9 = HourRange(ZonedDateTime.of(2024, 6, 15, 9, 0, 0, 0, ZoneOffset.UTC))
     * val hour12 = hour9.addHours(3) // 12시
     * ```
     */
    fun addHours(hours: Int): HourRange {
        val startHour = this.start.startOfHour()
        return HourRange(startHour.plusHours(hours.toLong()), calendar)
    }

    /**
     * 이전 시각 [HourRange]를 반환합니다.
     *
     * ```kotlin
     * val hour10 = HourRange(ZonedDateTime.of(2024, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC))
     * val hour9 = hour10.prevHour() // 9시
     * ```
     */
    fun prevHour(): HourRange = addHours(-1)

    /**
     * 다음 시각 [HourRange]를 반환합니다.
     *
     * ```kotlin
     * val hour9 = HourRange(ZonedDateTime.of(2024, 6, 15, 9, 0, 0, 0, ZoneOffset.UTC))
     * val hour10 = hour9.nextHour() // 10시
     * ```
     */
    fun nextHour(): HourRange = addHours(1)
}
