package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.logging.KLogging
import java.time.ZonedDateTime

/**
 * [ITimeLineMomentCollection] 의 기본 구현체
 */
@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
open class TimeLineMomentCollection protected constructor(
    private val moments: MutableList<ITimeLineMoment>,
): ITimeLineMomentCollection, MutableList<ITimeLineMoment> by moments {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(moments: MutableList<ITimeLineMoment> = mutableListOf()): TimeLineMomentCollection {
            return TimeLineMomentCollection(moments)
        }
    }

    override fun minOrNull(): ITimeLineMoment? = moments.minOrNull()

    override fun maxOrNull(): ITimeLineMoment? = moments.maxOrNull()

    override fun add(period: ITimePeriod) {
        addPeriod(period.start, period)
        addPeriod(period.end, period)
    }

    override fun addAll(periods: Collection<ITimePeriod>) {
        periods.forEach { add(it) }
    }

    override fun remove(period: ITimePeriod) {
        removePeriod(period.start, period)
        removePeriod(period.end, period)
    }

    override fun find(moment: ZonedDateTime): ITimeLineMoment? {
        return moments.find { it.moment == moment }
    }

    override fun contains(moment: ZonedDateTime): Boolean {
        return moments.any { it.moment == moment }
    }

    protected fun addPeriod(moment: ZonedDateTime, period: ITimePeriod) {
        var item = find(moment)
        if (item == null) {
            item = TimeLineMoment(moment)
            moments.add(item)
            moments.sort()
        }
        item.periods.add(period)
    }

    protected fun removePeriod(moment: ZonedDateTime, period: ITimePeriod) {
        val item = find(moment)
        if (item != null && item.periods.contains(period)) {
            item.periods.remove(period)
            if (item.periods.isEmpty()) {
                moments.remove(item)
            }
        }
    }
}
