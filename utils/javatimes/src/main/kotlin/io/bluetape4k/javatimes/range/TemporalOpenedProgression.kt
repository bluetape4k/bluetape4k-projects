package io.bluetape4k.javatimes.range

import io.bluetape4k.javatimes.isNegative
import io.bluetape4k.javatimes.isPositive
import io.bluetape4k.javatimes.isZero
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.hashOf
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount

/**
 * [TemporalOpenedProgression] 인스턴스를 생성합니다. 종료 값은 포함되지 않습니다.
 *
 * ```kotlin
 * val start = LocalDateTime.of(2024, 1, 1, 0, 0)
 * val end = LocalDateTime.of(2024, 1, 4, 0, 0)
 * val progression = temporalOpenedProgression(start, end, Duration.ofDays(1))
 * progression.toList() // [2024-01-01, 2024-01-02, 2024-01-03] (end 제외)
 * ```
 *
 * @param start T
 * @param endExclusive T
 * @param step TemporalAmount
 * @return TemporalOpenedProgression<T>
 */
fun <T> temporalOpenedProgression(
    start: T,
    endExclusive: T,
    step: TemporalAmount,
): TemporalOpenedProgression<T> where T: Temporal, T: Comparable<T> {
    return TemporalOpenedProgression.fromOpendRange(start, endExclusive, step)
}

/**
 * [Temporal] 의 범위를 나타내지만, 마지막 요소를 제외한 Open 된 Range를 표현합니다. `( min <= x < max )` 와 같습니다.
 *
 * ```kotlin
 * val start = LocalDateTime.of(2024, 1, 1, 0, 0)
 * val end = LocalDateTime.of(2024, 1, 4, 0, 0)
 * val progression = TemporalOpenedProgression.fromOpendRange(start, end, Duration.ofDays(1))
 * progression.toList() // [2024-01-01, 2024-01-02, 2024-01-03]
 * ```
 *
 * @param start 시작 시각 (포함)
 * @param endExclusive 종료 시각 (제외)
 * @param step 증가 단계
 */
open class TemporalOpenedProgression<T> protected constructor(
    start: T,
    endExclusive: T,
    step: TemporalAmount,
): TemporalClosedProgression<T>(start, endExclusive, step) where T: Temporal, T: Comparable<T> {

    companion object: KLogging() {
        @JvmStatic
        fun <T> fromOpendRange(
            start: T,
            endExclusive: T,
            step: TemporalAmount,
        ): TemporalOpenedProgression<T> where T: Temporal, T: Comparable<T> {
            assert(!step.isZero) { "step must be non-zero." }
            if (start != endExclusive) {
                assert((start < endExclusive) == (step.isPositive)) {
                    "start[$start]..endInclusive[$endExclusive]와 step[$step]이 잘못되었습니다."
                }
            }
            return TemporalOpenedProgression(start, endExclusive, step)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun sequence(): Sequence<T> = sequence seq@{
        fun canContinue(current: T): Boolean = when {
            step.isPositive -> current < last
            step.isNegative -> current > last
            else            -> false
        }

        var current = first

        while (canContinue(current)) {
            yield(current)
            current = current.plus(step) as T
        }
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is TemporalOpenedProgression<*> ->
            (isEmpty() && other.isEmpty()) ||
                    (first == other.first && last == other.last && step == other.step)

        else                            -> false
    }

    override fun hashCode(): Int = if (isEmpty()) -1 else hashOf(first, last, step)
}
