package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.ValueObject
import io.bluetape4k.javatimes.period.ITimePeriod
import java.time.ZonedDateTime

/**
 * ITimeLineMomentCollection
 */
interface ITimeLineMomentCollection: ValueObject, MutableList<ITimeLineMoment> {

    fun minOrNull(): ITimeLineMoment?

    fun maxOrNull(): ITimeLineMoment?

    fun add(period: ITimePeriod)

    fun addAll(periods: Collection<ITimePeriod>)

    fun remove(period: ITimePeriod)

    fun find(moment: ZonedDateTime): ITimeLineMoment?

    fun contains(moment: ZonedDateTime): Boolean

}
