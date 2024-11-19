package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.hashOf
import java.time.LocalTime

/**
 * 하루 중의 시간단위의 범위를 나타냅니다. 1시 ~ 4시 (start=1, end=3:59:59:999_999_999)
 *
 * @property start 시간 범위의 시작 시각 (포함)
 * @property end 완료 시각 (포함)
 */
open class HourRangeInDay(
    val start: LocalTime = LocalTime.MIN,
    val end: LocalTime = LocalTime.of(23, 0),
): AbstractValueObject(), Comparable<HourRangeInDay> {

    constructor(startHourOfDay: Int, endHourOfDay: Int = startHourOfDay)
            : this(LocalTime.of(startHourOfDay, 0), LocalTime.of(endHourOfDay, 0))

    override fun compareTo(other: HourRangeInDay): Int = start.compareTo(other.start)

    override fun hashCode(): Int = hashOf(start, end)

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun equalProperties(other: Any): Boolean =
        other is HourRangeInDay &&
                start == other.start &&
                end == other.end

    override fun buildStringHelper(): ToStringBuilder =
        super.buildStringHelper()
            .add("start", start)
            .add("end", end)
}
