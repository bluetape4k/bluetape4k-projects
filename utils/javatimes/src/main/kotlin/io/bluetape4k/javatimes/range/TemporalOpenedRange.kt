package io.bluetape4k.javatimes.range

import io.bluetape4k.logging.KLogging
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.Temporal

/**
 * 마지막 요소를 제외한 [Temporal]의 범위를 나타내는 Open Range 를 표현합니다.
 * `start <= x < endExclusive` 입니다.
 *
 * @param T
 * @constructor
 *
 * @param start         시작 시각
 * @param endExclusive  완료 시각 (제외)
 */
class TemporalOpenedRange<T>(start: T, endExclusive: T):
    TemporalOpenedProgression<T>(start, endExclusive, Duration.ofMillis(1)) where T: Temporal, T: Comparable<T> {

    companion object: KLogging() {
        @JvmField
        val EMPTY = TemporalOpenedRange<Instant>(
            Instant.ofEpochMilli(0L),
            Instant.ofEpochMilli(0L)
        )

        @JvmStatic
        fun <T> fromOpenedRange(
            start: T,
            endExclusive: T,
        ): TemporalOpenedRange<T> where T: Temporal, T: Comparable<T> {
            assert(start !is LocalDate) { "LocalDate는 지원하지 않습니다." }
            assert(start < endExclusive) { "start[$start] < endInclusive[$endExclusive]" }
            return TemporalOpenedRange(start, endExclusive)
        }
    }

    init {
        assert(start !is LocalDate) { "LocalDate는 지원하지 않습니다." }
    }

    val start: T get() = first

    val endExclusive: T get() = last

    fun contains(value: T): Boolean = first <= value && value < endExclusive

    override fun isEmpty(): Boolean = first >= last

    override fun toString(): String = "$first until $last step $step"
}
