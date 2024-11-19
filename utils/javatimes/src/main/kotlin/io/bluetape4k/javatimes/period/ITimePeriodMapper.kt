package io.bluetape4k.javatimes.period

import java.time.ZonedDateTime

/**
 * 기간의 시작 시각, 완료 시각에 대한 매핑을 수행하는 인터페이스.
 *
 * 시작, 완료 시각의 기간 내 포함 여부를 조절한다
 */
interface ITimePeriodMapper {

    fun mapStart(moment: ZonedDateTime): ZonedDateTime

    fun mapEnd(moment: ZonedDateTime): ZonedDateTime

    fun unmapStart(moment: ZonedDateTime): ZonedDateTime

    fun unmapEnd(moment: ZonedDateTime): ZonedDateTime

}
