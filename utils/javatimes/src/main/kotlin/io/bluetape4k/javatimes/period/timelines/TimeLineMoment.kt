package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.period.TimePeriodCollection
import java.time.ZonedDateTime

/**
 * [ITimeLineMoment]의 기본 구현체
 */
open class TimeLineMoment(override val moment: ZonedDateTime): AbstractValueObject(), ITimeLineMoment {

    override val periods: TimePeriodCollection = TimePeriodCollection()

    override val startCount: Long
        get() = periods.periods.count { it.start == moment }.toLong()

    override val endCount: Long
        get() = periods.periods.count { it.end == moment }.toLong()

    override fun compareTo(other: ITimeLineMoment): Int =
        moment.compareTo(other.moment)

    override fun equalProperties(other: Any): Boolean {
        return other is ITimeLineMoment && moment == other.moment
    }

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = moment.hashCode()

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("moment", moment)
            .add("startCount", startCount)
            .add("endCount", endCount)
            .add("periods", periods)
}
