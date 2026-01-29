package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.HoursPerDay
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.relativeDayPeriod
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

/**
 * 일 단위로 [dayCount] 만큼의 기간을 표현하는 클래스입니다.
 */
open class DayTimeRange(
    startTime: ZonedDateTime = todayZonedDateTime(),
    val dayCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): CalendarTimeRange(startTime.relativeDayPeriod(dayCount), calendar) {

    fun hourSequence(): Sequence<HourRange> =
        hourRanges(startDayOfStart, dayCount * HoursPerDay, calendar)

    fun hours(): List<HourRange> = hourSequence().toFastList()

    fun minuteSequence(): Sequence<MinuteRange> =
        hourSequence().flatMap { it.minuteSequence() }

    fun minutes(): List<MinuteRange> = minuteSequence().toFastList()
}
