package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.support.hashOf

/**
 * 시작/끝 인덱스로 표현되는 기본 구간 구현체입니다.
 *
 * ## 동작/계약
 * - [start]가 [end]보다 크면 빈 구간([isEmpty])으로 취급합니다.
 * - [compareTo]는 `start -> end` 순서로 정렬 기준을 제공합니다.
 * - 값 동등성은 [start], [end]만 비교합니다.
 *
 * ```kotlin
 * val interval = Interval(2, 5)
 * // interval.size == 4
 * ```
 */
open class Interval(
    override val start: Int,
    override val end: Int,
): AbstractValueObject(), Intervalable {

    companion object {
        @JvmField
        val EMPTY = Interval(1, 0)
    }

    /** 빈 구간 여부입니다. */
    val isEmpty: Boolean get() = start > end

    /** 다른 구간과 겹치는지 여부를 반환합니다. */
    fun overlapsWith(other: Interval): Boolean {
        return start < other.end && end >= other.start
    }

    /** 점 하나가 구간 안에 포함되는지 여부를 반환합니다. */
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

    override fun equals(other: Any?): Boolean = other?.let { equalProperties(it) } ?: false
    override fun hashCode(): Int = if (isEmpty) -1 else hashOf(start, end)
    override fun toString(): String = "Interval($start:$end)"
}
