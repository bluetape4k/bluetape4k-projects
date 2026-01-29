package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.ZonedDateTime

/**
 * 시간 단위의 범위를 나타내는 [HourTimeRange]의 컬렉션
 */
open class HourRangeCollection(
    startTime: ZonedDateTime = ZonedDateTime.now(),
    hourCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): HourTimeRange(startTime, hourCount, calendar) {

    constructor(
        year: Int,
        monthOfYear: Int = 1,
        dayOfMonth: Int = 1,
        hourOfDay: Int = 0,
        hourCount: Int = 1,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(zonedDateTimeOf(year, monthOfYear, dayOfMonth, hourOfDay), hourCount, calendar)

    fun hourSequence(): Sequence<HourRange> = hourRanges(startHourOfStart, hourCount, calendar)

    fun hours(): List<HourRange> = hourSequence().toFastList()

}
