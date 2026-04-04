package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

/**
 * 일 단위의 범위를 나타내는 [DayTimeRange]의 컬렉션입니다.
 *
 * ```kotlin
 * val days = DayRangeCollection(ZonedDateTime.of(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC), 7)
 * days.days().size // 7
 * days.daySequence().toList().map { it.dayOfMonth } // [15, 16, 17, 18, 19, 20, 21]
 * ```
 */
open class DayRangeCollection(
    startTime: ZonedDateTime = todayZonedDateTime(),
    dayCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): DayTimeRange(startTime, dayCount, calendar) {

    fun daySequence(): Sequence<DayRange> = dayRanges(startDayOfStart, dayCount, calendar)

    fun days(): List<DayRange> = daySequence().toList()
}
