package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 월 단위의 범위를 나타내는 [MonthRange]의 컬렉션
 */
open class MonthRangeCollection(
    startTime: ZonedDateTime = todayZonedDateTime(),
    monthCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): MonthTimeRange(startTime, monthCount, calendar) {

    /**
     * [year], [monthOfYear] 기준의 컬렉션을 생성합니다. 기본 zone은 UTC입니다.
     */
    constructor(year: Int, monthOfYear: Int, monthCount: Int = 1, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(year, monthOfYear), monthCount, calendar)

    constructor(
        year: Int,
        monthOfYear: Int,
        monthCount: Int = 1,
        zoneId: ZoneId,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(zonedDateTimeOf(year, monthOfYear, zoneId = zoneId), monthCount, calendar)

    fun monthSequence(): Sequence<MonthRange> = monthRanges(startDayOfStart, monthCount, calendar)

    fun months(): List<MonthRange> = monthSequence().toList()
}
