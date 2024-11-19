package io.bluetape4k.javatimes.range

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.Temporal

/**
 * A range of [Temporal] instance
 */
class TemporalClosedRange<T>(
    start: T,
    endInclusive: T,
): TemporalClosedProgression<T>(start, endInclusive, Duration.ofMillis(1)),
   ClosedRange<T>, Serializable
        where T: Temporal, T: Comparable<T> {

    companion object: KLogging() {
        @JvmField
        val EMPTY = TemporalClosedRange<Instant>(
            Instant.ofEpochMilli(0L),
            Instant.ofEpochMilli(0L)
        )

        fun <T> fromClosedRange(
            start: T,
            endInclusive: T,
        ): TemporalClosedRange<T> where T: Temporal, T: Comparable<T> {
            assert(start !is LocalDate) { "LocalDate는 지원하지 않습니다." }
            assert(start <= endInclusive) { "start[$start] <= endInclusive[$endInclusive]" }
            return TemporalClosedRange(start, endInclusive)
        }
    }

    init {
        assert(start !is LocalDate) { "LocalDate는 지원하지 않습니다." }
    }

    override val start: T get() = first

    override val endInclusive: T get() = last

    override fun contains(value: T): Boolean = value in first..last

    override fun isEmpty(): Boolean = first >= last

    override fun toString(): String = "$first..$last step $step"
}
