package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfWeekOfWeekyear
import java.time.ZonedDateTime

/**
 * 여러 주(week) 를 범위로 나타내는 클래스입니다.
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
    ): this(startOfWeekOfWeekyear(weekyear, weekOfWeekyear), weekCount, calendar)

    fun weekSequence(): Sequence<WeekRange> = weekRanges(start, weekCount, calendar)

    fun weeks(): List<WeekRange> = weekSequence().toFastList()
}
