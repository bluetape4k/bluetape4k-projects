package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MonthsPerQuarter
import io.bluetape4k.javatimes.Quarter
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfQuarter
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

/**
 * 한 분기 범위를 나타내는 클래스
 */
open class QuarterRange(
    startTime: ZonedDateTime = todayZonedDateTime(),
    calendar: ITimeCalendar = TimeCalendar.Default,
): QuarterTimeRange(startTime, 1, calendar) {

    constructor(
        year: Int,
        quarter: Quarter,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(startOfQuarter(year, quarter), calendar)

    val year: Int get() = startYear
    val quarter: Quarter get() = quarterOfStart

    fun addQuarters(quarters: Int): QuarterRange =
        QuarterRange(start.plusMonths(quarters.toLong() * MonthsPerQuarter), calendar)

    fun prevQuarter(): QuarterRange = addQuarters(-1)
    fun nextQuarter(): QuarterRange = addQuarters(1)
}
