package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.ZonedDateTime

/**
 * 월 단위의 범위를 나타내는 [MonthRange]의 컬렉션
 */
open class MonthRangeCollection(
    startTime: ZonedDateTime = todayZonedDateTime(),
    monthCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): MonthTimeRange(startTime, monthCount, calendar) {

    constructor(year: Int, monthOfYear: Int, monthCount: Int = 1, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(year, monthOfYear), monthCount, calendar)

    fun monthSequence(): Sequence<MonthRange> = monthRanges(startDayOfStart, monthCount, calendar)

    fun months(): List<MonthRange> = monthSequence().toList()
}
