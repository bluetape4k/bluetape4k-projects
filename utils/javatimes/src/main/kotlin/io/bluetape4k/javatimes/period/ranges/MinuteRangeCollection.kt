package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import java.time.ZonedDateTime

/**
 * 분 단위의 기간을 나타내는 [MinuteRange]의 컬렉션 클래스입니다.
 */
open class MinuteRangeCollection(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    minuteCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): MinuteTimeRange(startTime, minuteCount, calendar) {

    fun minuteSequence(): Sequence<MinuteRange> =
        minuteRanges(startMinuteOfStart, minuteCount, calendar)

    fun minutes(): List<MinuteRange> = minuteSequence().toFastList()
}
