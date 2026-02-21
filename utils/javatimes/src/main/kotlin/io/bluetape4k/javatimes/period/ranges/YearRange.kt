package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfYear
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * 한 해 (1 year) 범위를 나타내는 클래스입니다.
 */
open class YearRange(
    startTime: ZonedDateTime = nowZonedDateTime(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): YearTimeRange(startTime.startOfYear(), 1, calendar) {

    /**
     * [year] 기준의 [YearRange]를 생성합니다. 기본 zone은 UTC입니다.
     */
    constructor(
        year: Int,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(startOfYear(year, ZoneOffset.UTC), calendar)

    /**
     * [year], [zoneId] 기준의 [YearRange]를 생성합니다.
     */
    constructor(
        year: Int,
        zoneId: ZoneId,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(startOfYear(year, zoneId), calendar)

    fun addYears(years: Int): YearRange = YearRange(start.plusYears(years.toLong()), calendar)

    fun prevYear(): YearRange = addYears(-1)
    fun nextYear(): YearRange = addYears(1)
}
