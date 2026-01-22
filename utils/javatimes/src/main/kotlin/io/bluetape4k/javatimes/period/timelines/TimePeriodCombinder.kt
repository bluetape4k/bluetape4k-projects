package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.ITimePeriodMapper
import io.bluetape4k.javatimes.period.TimePeriodCollection

/**
 * TimePeriodCombiner
 */
class TimePeriodCombiner<T: ITimePeriod>(val mapper: ITimePeriodMapper? = null) {

    fun combinePeriods(periods: Collection<ITimePeriod>): ITimePeriodCollection {
        return TimeLine<T>(TimePeriodCollection.ofAll(periods), null, mapper).combinePeriods()
    }
}
