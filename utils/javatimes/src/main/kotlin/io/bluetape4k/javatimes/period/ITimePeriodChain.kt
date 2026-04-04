package io.bluetape4k.javatimes.period

import java.time.Duration
import java.time.ZonedDateTime

/**
 * 여러 기간[ITimePeriod] 들의 체인 형태의 자료구조를 나타냅니다.
 *
 * ```kotlin
 * val chain = TimePeriodChain()
 * val now = ZonedDateTime.now()
 * chain.add(TimeBlock(now, Duration.ofHours(2)))
 * chain.add(TimeBlock(now.plusHours(2), Duration.ofHours(3)))
 * chain.headOrNull()?.start // now
 * chain.lastOrNull()?.end   // now + 5h
 * ```
 */
interface ITimePeriodChain: ITimePeriodContainer {

    /**
     * 체인의 첫 번째 기간을 반환합니다. 비어있으면 null을 반환합니다.
     *
     * ```kotlin
     * val chain = TimePeriodChain()
     * chain.add(TimeBlock(ZonedDateTime.now(), Duration.ofHours(1)))
     * chain.headOrNull()?.start // ZonedDateTime.now()
     * ```
     */
    fun headOrNull(): ITimePeriod? = periods.firstOrNull()

    /**
     * 체인의 마지막 기간을 반환합니다. 비어있으면 null을 반환합니다.
     *
     * ```kotlin
     * val chain = TimePeriodChain()
     * val now = ZonedDateTime.now()
     * chain.add(TimeBlock(now, Duration.ofHours(1)))
     * chain.add(TimeBlock(now.plusHours(1), Duration.ofHours(2)))
     * chain.lastOrNull()?.end // now + 3h
     * ```
     */
    fun lastOrNull(): ITimePeriod? = periods.lastOrNull()

    /**
     * [moment] 이전에 빈 기간이 있는지 확인한다. 없으면 예외를 발생시킨다.
     *
     * ```kotlin
     * val chain = TimePeriodChain()
     * val now = ZonedDateTime.now()
     * chain.add(TimeBlock(now.plusHours(2), Duration.ofHours(1)))
     * chain.assertSpaceBefore(now.plusHours(2), Duration.ofHours(1)) // 공간이 충분하면 정상
     * ```
     */
    fun assertSpaceBefore(moment: ZonedDateTime, duration: Duration)

    /**
     * [moment] 이후에 빈 기간이 있는지 확인한다. 없으면 예외를 발생시킨다.
     *
     * ```kotlin
     * val chain = TimePeriodChain()
     * val now = ZonedDateTime.now()
     * chain.add(TimeBlock(now, Duration.ofHours(1)))
     * chain.assertSpaceAfter(now.plusHours(1), Duration.ofHours(2)) // 공간이 충분하면 정상
     * ```
     */
    fun assertSpaceAfter(moment: ZonedDateTime, duration: Duration)
}
