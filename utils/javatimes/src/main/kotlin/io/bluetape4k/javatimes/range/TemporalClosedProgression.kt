package io.bluetape4k.javatimes.range

import io.bluetape4k.javatimes.isNegative
import io.bluetape4k.javatimes.isPositive
import io.bluetape4k.javatimes.isZero
import io.bluetape4k.javatimes.millis
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.hashOf
import java.io.Serializable
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount

/**
 * Create [TemporalClosedProgression] instance
 * @param start T
 * @param endInclusive T
 * @param step TemporalAmount
 * @return TemporalClosedProgression<T>
 */
fun <T> temporalClosedProgressionOf(
    start: T,
    endInclusive: T,
    step: TemporalAmount,
): TemporalClosedProgression<T> where T: Temporal, T: Comparable<T> {
    return TemporalClosedProgression.fromClosedRange(start, endInclusive, step)
}

/**
 * [Temporal] 의 범위를 나타내지만, 마지막 요소를 포함한 Closed Range를 표현합니다. `( min <= x <= max )` 와 같습니다.
 */
open class TemporalClosedProgression<T> protected constructor(
    start: T,
    endInclusive: T,
    val step: TemporalAmount,
): Iterable<T>, Serializable where T: Temporal, T: Comparable<T> {

    companion object: KLogging() {
        @JvmStatic
        fun <T> fromClosedRange(
            start: T,
            endInclusive: T,
            step: TemporalAmount,
        ): TemporalClosedProgression<T> where T: Temporal, T: Comparable<T> {
            assert(!step.isZero) { "step must be non-zero." }
            if (start != endInclusive) {
                assert((start <= endInclusive) == (step.isPositive)) {
                    "start[$start]..endInclusive[$endInclusive]와 step[$step]이 잘못되었습니다."
                }
            }
            return TemporalClosedProgression(start, endInclusive, step)
        }
    }

    init {
        assert(!step.isZero) { "step must be non-zero." }
        if (start != endInclusive) {
            assert((start <= endInclusive) == (step.isPositive)) {
                "start[$start]..endInclusive[$endInclusive]와 step[$step]이 잘못되었습니다."
            }
        }
    }

    val first: T = start

    open val last: T = getProgressionLastElement(start, endInclusive, step.millis)

    open fun isEmpty(): Boolean = if (step.isPositive) first > last else first < last

    @Suppress("UNCHECKED_CAST")
    open fun sequence(): Sequence<T> = sequence seq@{
        fun canContinue(current: T): Boolean = when {
            step.isPositive -> current <= last
            step.isNegative -> current >= last
            else            -> false
        }

        var current = first

        while (canContinue(current)) {
            yield(current)
            current = current.plus(step) as T
        }
    }

    override fun iterator(): Iterator<T> = sequence().iterator()

    override fun equals(other: Any?): Boolean = when (other) {
        is TemporalClosedProgression<*> -> (isEmpty() && other.isEmpty()) ||
                (first == other.first && last == other.last && step == other.step)

        else                            -> false
    }

    override fun hashCode(): Int = if (isEmpty()) -1 else hashOf(first, last, step)

    override fun toString(): String = when {
        step.isZero     -> "$first..$last"
        step.isPositive -> "$first..$last step $step"
        else            -> "$first..$last step ${step.millis}"
    }
}
