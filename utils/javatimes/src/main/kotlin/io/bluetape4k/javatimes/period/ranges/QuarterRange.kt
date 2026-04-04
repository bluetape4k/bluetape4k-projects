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
 *
 * ```kotlin
 * val q1 = QuarterRange(2024, Quarter.Q1) // 2024년 1분기 (1~3월)
 * val q2 = q1.nextQuarter() // 2024년 2분기
 * val q0 = q1.prevQuarter() // 2023년 4분기
 * ```
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

    /**
     * 현재 분기에서 [quarters]만큼 더한 [QuarterRange]를 반환합니다.
     *
     * ```kotlin
     * val q1 = QuarterRange(2024, Quarter.Q1)
     * val q3 = q1.addQuarters(2) // 2024년 3분기
     * ```
     */
    fun addQuarters(quarters: Int): QuarterRange =
        QuarterRange(start.plusMonths(quarters.toLong() * MonthsPerQuarter), calendar)

    /**
     * 이전 분기 [QuarterRange]를 반환합니다.
     *
     * ```kotlin
     * val q2 = QuarterRange(2024, Quarter.Q2)
     * val q1 = q2.prevQuarter() // 2024년 1분기
     * ```
     */
    fun prevQuarter(): QuarterRange = addQuarters(-1)

    /**
     * 다음 분기 [QuarterRange]를 반환합니다.
     *
     * ```kotlin
     * val q1 = QuarterRange(2024, Quarter.Q1)
     * val q2 = q1.nextQuarter() // 2024년 2분기
     * ```
     */
    fun nextQuarter(): QuarterRange = addQuarters(1)
}
