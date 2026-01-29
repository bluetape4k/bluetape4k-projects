package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.MinutesPerHour
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeHourPeriod
import java.time.ZonedDateTime

/**
 * 시간 단위로 기간을 표현하는 클래스입니다.
 */
open class HourTimeRange(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    val hourCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(startTime.relativeHourPeriod(hourCount), calendar) {

    val hourOfDayOfEnd: Int get() = end.hour

    fun minuteSequence(): Sequence<MinuteRange> =
        minuteRanges(startMinuteOfStart, hourCount * MinutesPerHour, calendar)

    fun minutes(): List<MinuteRange> = minuteSequence().toFastList()
}
