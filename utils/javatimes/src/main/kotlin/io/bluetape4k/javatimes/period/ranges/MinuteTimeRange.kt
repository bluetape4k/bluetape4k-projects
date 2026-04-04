package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeMinutePeriod
import java.time.ZonedDateTime

/**
 * 분 단위로 기간을 표현하는 클래스입니다.
 *
 * ```kotlin
 * val now = ZonedDateTime.now()
 * val range = MinuteTimeRange(now, minuteCount = 30)
 * range.minuteCount         // 30
 * range.minuteOfHourOfEnd   // (now + 30min).minute
 * ```
 */
open class MinuteTimeRange(
    moment: ZonedDateTime = ZonedDateTime.now(),
    val minuteCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(moment.relativeMinutePeriod(minuteCount), calendar) {

    val minuteOfHourOfEnd: Int get() = end.minute
}
