package io.bluetape4k.javatimes.range

import io.bluetape4k.javatimes.plus
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.hashOf
import java.time.Duration
import java.util.*

/**
 * [Date] 단위의 Progression을 생성합니다. ([DateGenericProgression])
 *
 * ```kotlin
 * val start = Date(0L)
 * val end = Date(3 * 24 * 3600 * 1000L)
 * val progression = dateProgressionOf(start, end, Duration.ofDays(1))
 * progression.toList().size // 4 (0일, 1일, 2일, 3일 포함)
 * ```
 *
 * @param start        시작 Date
 * @param endInclusive 끝 Date
 * @param step         증감량 (기본값: 1ms)
 * @return [DateGenericProgression] 인스턴스
 */
fun <T: Date> dateProgressionOf(
    start: T,
    endInclusive: T,
    step: Duration = Duration.ofMillis(1),
): DateGenericProgression<T> {
    assert(!step.isZero) { "step must be non-zero" }
    if (start != endInclusive) {
        assert((start < endInclusive) == step.isPositive) {
            "step의 증감이 반대가 되면 안됩니다. start=$start, endInclusive=$endInclusive, step=$step"
        }
    }

    return DateGenericProgression.fromClosedRange(start, endInclusive, step)
}

/**
 * [Date]를 요소로 하는 Progression 클래스입니다.
 *
 * ```kotlin
 * val start = Date(0L)
 * val end = Date(2 * 24 * 3600 * 1000L)
 * val progression = DateGenericProgression(start, end, Duration.ofDays(1))
 * progression.toList().size // 3 (0일, 1일, 2일 포함)
 * ```
 *
 * @property first first value of progression
 * @property last  last value of progression
 * @property step  progression step
 */
open class DateGenericProgression<out T: Date> internal constructor(
    start: T,
    endInclusive: T,
    val step: Duration,
): Iterable<T> {

    init {
        assert(!step.isZero) { "step must be non-zero" }
        if (start != endInclusive) {
            assert((start <= endInclusive) == step.isPositive) {
                "step의 증감이 반대가 되면 안됩니다. start=$start, endInclusive=$endInclusive, step=$step"
            }
        }
    }

    companion object: KLogging() {
        @JvmStatic
        operator fun <T: Date> invoke(
            start: T,
            endInclusive: T,
            step: Duration = Duration.ofMillis(1),
        ): DateGenericProgression<T> {
            return DateGenericProgression(start, endInclusive, step)
        }

        @JvmStatic
        fun <T: Date> fromClosedRange(
            start: T,
            endInclusive: T,
            step: Duration = Duration.ofMillis(1),
        ): DateGenericProgression<T> {
            return DateGenericProgression(start, endInclusive, step)
        }
    }

    val first: T = start

    val last: T = getProgressionLastElement(start, endInclusive, step.toMillis())

    // 시작과 끝이 같다면 하나의 값을 가진다
    open fun isEmpty(): Boolean = if (step.isPositive) first > last else first < last

    override fun equals(other: Any?): Boolean = when (other) {
        !is DateGenericProgression<*> -> false
        else                          -> (isEmpty() && other.isEmpty()) ||
                (first == other.first && last == other.last && step == other.step)
    }

    override fun hashCode(): Int = when {
        isEmpty() -> -1
        else      -> hashOf(first, last, step)
    }

    override fun toString(): String = when {
        step.isZero     -> "$first..$last"
        step.isPositive -> "$first..$last step $step"
        else            -> "$first downTo $last step ${step.negated()}"
    }

    @Suppress("UNCHECKED_CAST")
    fun sequence(): Sequence<T> = sequence seq@{
        if (step.isZero) {
            yield(first)
            return@seq
        }

        fun canContinue(current: T): Boolean {
            return when {
                step.isPositive -> current <= last
                step.isNegative -> current >= last
                else            -> false
            }
        }

        var current = first

        while (canContinue(current)) {
            yield(current)
            current = (current + step) as T
        }
    }

    override fun iterator(): Iterator<T> = sequence().iterator()
}
