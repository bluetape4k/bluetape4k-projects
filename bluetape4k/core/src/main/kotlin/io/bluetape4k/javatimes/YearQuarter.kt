package io.bluetape4k.javatimes

import java.io.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * 년도(Year) 와 분기(Quarter)를 표현하는 클래스입니다.
 *
 * 년도와 분기 정보를 함께 보관하며, [LocalDateTime], [OffsetDateTime], [ZonedDateTime]으로부터
 * 생성할 수 있습니다. 분산 캐시 직렬화를 위해 [java.io.Serializable]을 구현합니다.
 *
 * ```kotlin
 * // 명시적 년도·분기로 생성
 * val yq1 = YearQuarter(2024, Quarter.Q1)
 * yq1.year    // 2024
 * yq1.quarter // Quarter.Q1
 *
 * // LocalDateTime으로 생성 (4월 → Q2)
 * val ldt = LocalDateTime.of(2024, 4, 15, 0, 0)
 * val yq2 = YearQuarter(ldt)
 * yq2.quarter // Quarter.Q2
 *
 * // ZonedDateTime으로 생성 (10월 → Q4)
 * val zdt = ZonedDateTime.of(2024, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC)
 * val yq4 = YearQuarter(zdt)
 * yq4.quarter // Quarter.Q4
 *
 * // data class 동등성 비교
 * YearQuarter(2024, Quarter.Q1) == YearQuarter(2024, Quarter.Q1) // true
 * YearQuarter(2024, Quarter.Q1) == YearQuarter(2024, Quarter.Q2) // false
 * ```
 *
 * @param year 년도
 * @param quarter 분기
 */
data class YearQuarter(
    val year: Int,
    val quarter: Quarter,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    constructor(moment: LocalDateTime) : this(moment.year, Quarter.ofMonth(moment.monthValue))
    constructor(moment: OffsetDateTime) : this(moment.year, Quarter.ofMonth(moment.monthValue))
    constructor(moment: ZonedDateTime) : this(moment.year, Quarter.ofMonth(moment.monthValue))
}
