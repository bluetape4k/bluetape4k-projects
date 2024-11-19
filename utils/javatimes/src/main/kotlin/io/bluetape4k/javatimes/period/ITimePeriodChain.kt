package io.bluetape4k.javatimes.period

import java.time.Duration
import java.time.ZonedDateTime

/**
 * 여러 기간[ITimePeriod] 들의 체인 형태의 자료구조를 나타냅니다.
 */
interface ITimePeriodChain: ITimePeriodContainer {

    fun headOrNull(): ITimePeriod? = periods.firstOrNull()

    fun lastOrNull(): ITimePeriod? = periods.lastOrNull()

    /**
     * [moment] 이전에 빈 기간이 있는지 확인한다. 없으면 예외를 발생시킨다.
     */
    fun assertSpaceBefore(moment: ZonedDateTime, duration: Duration)

    /**
     * [moment] 이후에 빈 기간이 있는지 확인한다. 없으면 예외를 발생시킨다.
     */
    fun assertSpaceAfter(moment: ZonedDateTime, duration: Duration)
}
