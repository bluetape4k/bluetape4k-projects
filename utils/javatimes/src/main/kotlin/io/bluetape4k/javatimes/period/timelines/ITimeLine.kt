package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.ValueObject
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.ITimePeriodContainer
import io.bluetape4k.javatimes.period.ITimePeriodMapper

/**
 * [ITimePeriod] 컬렉션을 가지며, 이를 통해 여러 기간에 대한 Union, Intersection, Gap 등을 구할 수 있도록 합니다.
 *
 * ```kotlin
 * val container = TimePeriodCollection()
 * container.add(TimeRange(start1, end1))
 * container.add(TimeRange(start2, end2))
 * val timeLine = TimeLine<ITimePeriod>(container)
 * val combined = timeLine.combinePeriods() // 겹치는 기간 결합
 * val gaps = timeLine.calculateGaps()      // 기간 사이 빈 간격
 * ```
 */
interface ITimeLine: ValueObject {

    val periods: ITimePeriodContainer

    val limits: ITimePeriod

    val mapper: ITimePeriodMapper?

    /**
     * 기간들을 결합하여 겹치는 부분을 하나로 합칩니다.
     *
     * ```kotlin
     * val combined = timeLine.combinePeriods() // overlapping periods merged
     * ```
     */
    fun combinePeriods(): ITimePeriodCollection

    /**
     * 기간들의 교집합을 계산합니다.
     *
     * ```kotlin
     * val intersected = timeLine.intersectPeriods()
     * ```
     */
    fun intersectPeriods(): ITimePeriodCollection

    /**
     * 기간들 사이의 빈 간격을 계산합니다.
     *
     * ```kotlin
     * val gaps = timeLine.calculateGaps()
     * ```
     */
    fun calculateGaps(): ITimePeriodCollection

}
