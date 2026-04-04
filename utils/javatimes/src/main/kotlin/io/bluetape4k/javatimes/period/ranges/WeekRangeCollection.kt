package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import java.time.ZonedDateTime

/**
 * 여러 주(week) 를 범위로 나타내는 클래스입니다.
 *
 * ```kotlin
 * val weeks = WeekRangeCollection(2024, 1, 4) // 2024년 1주차부터 4주간
 * weeks.weeks().size // 4
 * ```
 */
open class WeekRangeCollection(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    weekCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): WeekTimeRange(startTime, weekCount, calendar) {

    constructor(
        weekyear: Int,
        weekOfWeekyear: Int,
        weekCount: Int = 1,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(calendar.startOfYearWeek(weekyear, weekOfWeekyear), weekCount, calendar)

    fun weekSequence(): Sequence<WeekRange> = weekRanges(start, weekCount, calendar)

    fun weeks(): List<WeekRange> = weekSequence().toList()
}
