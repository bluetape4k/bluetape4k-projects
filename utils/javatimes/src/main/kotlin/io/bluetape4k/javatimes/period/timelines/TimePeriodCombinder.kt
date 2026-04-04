package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.ITimePeriodMapper
import io.bluetape4k.javatimes.period.TimePeriodCollection

/**
 * 여러 기간을 결합하는 클래스
 *
 * ```kotlin
 * val combiner = TimePeriodCombiner<ITimePeriod>()
 * val periods = listOf(
 *     TimeRange(start1, end1),
 *     TimeRange(start2, end2)  // 겹치면 하나로 합쳐짐
 * )
 * val combined = combiner.combinePeriods(periods)
 * ```
 */
class TimePeriodCombiner<T: ITimePeriod>(val mapper: ITimePeriodMapper? = null) {

    /**
     * 여러 기간을 결합하여 겹치는 기간을 하나로 합칩니다.
     *
     * ```kotlin
     * val combiner = TimePeriodCombiner<ITimePeriod>()
     * val combined = combiner.combinePeriods(
     *     listOf(TimeRange(start1, end2), TimeRange(start2, end3))
     * ) // 겹치면 [start1, end3] 하나로 합쳐짐
     * ```
     */
    fun combinePeriods(periods: Collection<ITimePeriod>): ITimePeriodCollection {
        return TimeLine<T>(TimePeriodCollection.ofAll(periods), null, mapper).combinePeriods()
    }
}
