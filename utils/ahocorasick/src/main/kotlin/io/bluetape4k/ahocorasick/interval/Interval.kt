package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.support.hashOf

open class Interval(
    override val start: Int,
    override val end: Int,
): AbstractValueObject(), Intervalable {

    companion object {
        @JvmField
        val EMPTY = Interval(1, 0)
    }

    val isEmpty: Boolean get() = start > end

    fun overlapsWith(other: Interval): Boolean {
        return start < other.end && end >= other.start
    }

    fun overlapsWith(point: Int): Boolean {
        return point in start..end
    }

    override fun compareTo(other: Intervalable): Int {
        var comparison = start - other.start
        if (comparison == 0) {
            comparison = end - other.end
        }
        return comparison
    }

    override fun equalProperties(other: Any): Boolean =
        other is Intervalable &&
                start == other.start &&
                end == other.end

    override fun equals(other: Any?): Boolean = other != null && equalProperties(other)
    override fun hashCode(): Int = if (isEmpty) -1 else hashOf(start, end)
    override fun toString(): String = "$start:$end"
}
