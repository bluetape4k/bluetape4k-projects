package io.bluetape4k.javatimes.range

import io.bluetape4k.logging.KLogging
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.Temporal

/**
 * [Temporal] 기반의 closed range 입니다.
 *
 * ## 지원 타입
 * - 현재는 epoch-millis 기반 순회가 가능한 시간 타입을 대상으로 설계되어 있습니다.
 * - `Instant`, `ZonedDateTime`, `LocalDateTime`, `OffsetDateTime`, `Date`, `Timestamp` 계열 사용을 권장합니다.
 * - `LocalDate`는 지원하지 않습니다.
 */
class TemporalClosedRange<T>(
    start: T,
    endInclusive: T,
): TemporalClosedProgression<T>(start, endInclusive, Duration.ofMillis(1)), ClosedRange<T>
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

    override fun contains(value: T): Boolean = first <= value && value <= last

    override fun isEmpty(): Boolean = first > last

    override fun toString(): String = "$first..$last step $step"
}
