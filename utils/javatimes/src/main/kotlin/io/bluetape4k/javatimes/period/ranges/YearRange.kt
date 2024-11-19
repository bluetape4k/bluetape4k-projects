package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import java.time.ZonedDateTime

/**
 * 한 해 (1 year) 범위를 나타내는 클래스입니다.
 */
open class YearRange(
    year: Int = nowZonedDateTime().year,
    calendar: ITimeCalendar = TimeCalendar.Default,
): YearTimeRange(year, 1, calendar) {

    constructor(
        moment: ZonedDateTime,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(moment.year, calendar)

    fun addYears(years: Int): YearRange = YearRange(year + years, calendar)

    fun prevYear(): YearRange = addYears(-1)
    fun nextYear(): YearRange = addYears(1)
}
