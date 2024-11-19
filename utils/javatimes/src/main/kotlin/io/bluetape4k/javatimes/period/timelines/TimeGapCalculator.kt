package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import io.bluetape4k.javatimes.period.ITimePeriodContainer
import io.bluetape4k.javatimes.period.ITimePeriodMapper

open class TimeGapCalculator<T: ITimePeriod>(val mapper: ITimePeriodMapper? = null) {

    fun gaps(excludePeriods: ITimePeriodContainer, limits: ITimePeriod? = null): ITimePeriodCollection =
        TimeLine<T>(excludePeriods, limits, mapper).calculateGaps()

}
