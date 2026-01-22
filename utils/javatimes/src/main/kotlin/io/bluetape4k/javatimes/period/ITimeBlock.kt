package io.bluetape4k.javatimes.period

import java.time.Duration
import java.time.ZonedDateTime

/**
 * 시간 블록을 나타내는 최상위 인터페이스
 */
interface ITimeBlock: ITimePeriod {

    override var start: ZonedDateTime
    override var end: ZonedDateTime
    override var duration: Duration

    fun setup(newStart: ZonedDateTime, newDuration: Duration)

    /** 시작 시점을 기준으로 기간을 설정하여, 종료 시각을 변경합니다 */
    fun durationFromStart(newDuration: Duration)

    /** 종료 시점을 기준으로 기간을 설정하여, 시작 시각을 변경합니다 */
    fun durationFromEnd(newDuration: Duration)

}
