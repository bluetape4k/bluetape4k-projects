package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.javatimes.MaxDaysPerMonth

/**
 * 한달 동안의 날짜 간견을 나타냅니다.
 * (예: 5일부터 21일까지)
 */
open class DayRangeInMonth(
    val startDayOfMonth: Int = 1,
    val endDayOfMonth: Int = MaxDaysPerMonth,
): AbstractValueObject(), Comparable<DayRangeInMonth> {

    init {
        require(startDayOfMonth in 1..MaxDaysPerMonth) { "startDayOfMonth[$startDayOfMonth] must be 1..31" }
        require(endDayOfMonth in 1..MaxDaysPerMonth) { "startDayOfMonth[$startDayOfMonth] must be 1..31" }
        require(startDayOfMonth <= endDayOfMonth) {
            "startDayOfMonth[$startDayOfMonth] must be less than or equal endDayOfMonth[$endDayOfMonth"
        }
    }

    val isSingleDay: Boolean get() = startDayOfMonth == endDayOfMonth

    fun hasInside(dayOfMonth: Int): Boolean = dayOfMonth in startDayOfMonth..endDayOfMonth

    fun assertValidDayRange(dayOfMonth: Int) {
        check(dayOfMonth in 1..MaxDaysPerMonth) {
            "dayOfMonth[$dayOfMonth]는 1..$MaxDaysPerMonth 에 있는 값이어야 합니다."
        }
    }

    override fun compareTo(other: DayRangeInMonth): Int =
        startDayOfMonth.compareTo(other.startDayOfMonth)

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int {
        return startDayOfMonth + endDayOfMonth * 100 // super<AbstractValueObject>.hashCode()
    }

    override fun equalProperties(other: Any): Boolean {
        return other is DayRangeInMonth &&
                startDayOfMonth == other.startDayOfMonth &&
                endDayOfMonth == other.endDayOfMonth

    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("startDayOfMonth", startDayOfMonth)
            .add("endDayOfMonth", endDayOfMonth)
    }
}
