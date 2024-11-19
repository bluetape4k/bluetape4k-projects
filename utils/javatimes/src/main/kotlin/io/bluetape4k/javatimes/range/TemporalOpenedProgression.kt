package io.bluetape4k.javatimes.range

import io.bluetape4k.javatimes.isNegative
import io.bluetape4k.javatimes.isPositive
import io.bluetape4k.javatimes.isZero
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.areEquals
import io.bluetape4k.support.hashOf
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount

/**
 * Create [TemporalOpenedProgression] instance
 * @param start T
 * @param endExclusive T
 * @param step TemporalAmount
 * @return TemporalClosedProgression<T>
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
 * @param start
 * @param endExclusive
 * @param step
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
                    (areEquals(first, other.first) &&
                            areEquals(last, other.last) &&
                            areEquals(step, other.step))

        else                            -> false
    }

    override fun hashCode(): Int = hashOf(first, last, step)
}
