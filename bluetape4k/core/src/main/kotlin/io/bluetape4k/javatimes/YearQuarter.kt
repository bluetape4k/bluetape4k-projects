package io.bluetape4k.javatimes

import java.io.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * 년도(Year) 와 분기(Quarter)를 표현하는 클래스입니다.
 *
 * @param year 년도
 * @param quarter 분기
 */
data class YearQuarter(
    val year: Int,
    val quarter: Quarter,
): Serializable {

    constructor(moment: LocalDateTime): this(moment.year, Quarter.ofMonth(moment.monthValue))
    constructor(moment: OffsetDateTime): this(moment.year, Quarter.ofMonth(moment.monthValue))
    constructor(moment: ZonedDateTime): this(moment.year, Quarter.ofMonth(moment.monthValue))

}
