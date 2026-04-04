package io.bluetape4k.javatimes.range

import io.bluetape4k.logging.KLogging
import java.time.Duration
import java.util.*

/**
 * [Date] 를 요소로하는 [ClosedRange]`<T>` 구현체입니다.
 *
 * ```kotlin
 * val range = DateGenericRange(Date(0L), Date(1000L))
 * range.start         // Date(0L)
 * range.endInclusive  // Date(1000L)
 * range.isEmpty()     // false
 * range.contains(Date(500L)) // true
 * range.contains(Date(2000L)) // false
 * ```
 *
 * @param start         start date
 * @param endInclusive  end date (inclusive)
 */
class DateGenericRange<T: Date>(
    start: T,
    endInclusive: T,
): DateGenericProgression<T>(start, endInclusive, Duration.ofMillis(1)), ClosedRange<T> {

    companion object: KLogging() {
        @JvmField
        val EMPTY: DateGenericRange<Date> = DateGenericRange(Date(0L), Date(0L))

        @JvmStatic
        fun <T: Date> fromClosedRange(start: T, endInclusive: T): DateGenericRange<T> {
            return DateGenericRange(start, endInclusive)
        }
    }

    override val start: T get() = first

    override val endInclusive: T get() = last

    override fun isEmpty(): Boolean = first > last

    override fun contains(value: T): Boolean = containsDate(value as Date)

    fun containsDate(value: Date): Boolean = first.time <= value.time && value.time <= last.time

    override fun toString(): String = "$first..$last"
}
