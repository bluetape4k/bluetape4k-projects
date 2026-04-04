package io.bluetape4k.javatimes.period

import java.time.Duration
import java.time.ZonedDateTime

/**
 * 시간 블록을 나타내는 최상위 인터페이스
 *
 * ```kotlin
 * val block: ITimeBlock = TimeBlock(ZonedDateTime.now(), Duration.ofHours(2))
 * block.duration // Duration.ofHours(2)
 * block.durationFromStart(Duration.ofHours(3)) // 종료시각을 start+3h로 변경
 * ```
 */
interface ITimeBlock: ITimePeriod {

    override var start: ZonedDateTime
    override var end: ZonedDateTime
    override var duration: Duration

    /**
     * 시작 시각과 기간으로 블록을 재설정합니다.
     *
     * ```kotlin
     * val block = TimeBlock(ZonedDateTime.now(), Duration.ofHours(1))
     * block.setup(ZonedDateTime.now().plusDays(1), Duration.ofHours(2))
     * ```
     */
    fun setup(newStart: ZonedDateTime, newDuration: Duration)

    /**
     * 시작 시점을 기준으로 기간을 설정하여, 종료 시각을 변경합니다
     *
     * ```kotlin
     * val block = TimeBlock(ZonedDateTime.now(), Duration.ofHours(1))
     * block.durationFromStart(Duration.ofHours(3)) // end = start + 3h
     * ```
     */
    fun durationFromStart(newDuration: Duration)

    /**
     * 종료 시점을 기준으로 기간을 설정하여, 시작 시각을 변경합니다
     *
     * ```kotlin
     * val block = TimeBlock(ZonedDateTime.now(), ZonedDateTime.now().plusHours(5))
     * block.durationFromEnd(Duration.ofHours(2)) // start = end - 2h
     * ```
     */
    fun durationFromEnd(newDuration: Duration)

}
