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
 *
 * ```kotlin
 * val yearRange = YearRange(2024) // 2024년 전체 기간
 * val next = yearRange.nextYear() // 2025년
 * val prev = yearRange.prevYear() // 2023년
 * ```
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

    /**
     * 현재 연도에서 [years]만큼 더한 [YearRange]를 반환합니다.
     *
     * ```kotlin
     * val yr2024 = YearRange(2024)
     * val yr2026 = yr2024.addYears(2) // 2026년
     * ```
     */
    fun addYears(years: Int): YearRange = YearRange(start.plusYears(years.toLong()), calendar)

    /**
     * 이전 연도 [YearRange]를 반환합니다.
     *
     * ```kotlin
     * val yr2024 = YearRange(2024)
     * val yr2023 = yr2024.prevYear() // 2023년
     * ```
     */
    fun prevYear(): YearRange = addYears(-1)

    /**
     * 다음 연도 [YearRange]를 반환합니다.
     *
     * ```kotlin
     * val yr2024 = YearRange(2024)
     * val yr2025 = yr2024.nextYear() // 2025년
     * ```
     */
    fun nextYear(): YearRange = addYears(1)
}
