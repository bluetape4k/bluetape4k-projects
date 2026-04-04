package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.Quarter
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfQuarter
import io.bluetape4k.javatimes.todayZonedDateTime
import java.time.ZonedDateTime

/**
 * 여러 분기를 기간으로 나타내는 클래스입니다.
 *
 * ```kotlin
 * val quarters = QuarterRangeCollection(2024, Quarter.Q1, 3) // 2024년 1~3분기
 * quarters.quarters().size // 3
 * quarters.quarterSequence().toList().map { it.quarter } // [Q1, Q2, Q3]
 * ```
 */
open class QuarterRangeCollection(
    startTime: ZonedDateTime = todayZonedDateTime(),
    quarterCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): QuarterTimeRange(startTime, quarterCount, calendar) {

    constructor(
        year: Int,
        quarter: Quarter,
        quarterCount: Int = 1,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(startOfQuarter(year, quarter), quarterCount, calendar)

    fun quarterSequence(): Sequence<QuarterRange> = quarterRanges(start, quarterCount, calendar)

    fun quarters(): List<QuarterRange> = quarterSequence().toList()
}
