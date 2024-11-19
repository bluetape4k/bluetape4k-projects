package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.ValueObject
import io.bluetape4k.javatimes.period.ITimePeriodCollection
import java.time.ZonedDateTime

/**
 * ITimeLineMoment
 */
interface ITimeLineMoment: ValueObject, Comparable<ITimeLineMoment> {

    val moment: ZonedDateTime

    val periods: ITimePeriodCollection

    val startCount: Long

    val endCount: Long

}
