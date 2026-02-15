package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * Flow를 주어진 [initialDelay] 이후에 주어진 [delay] 간격으로 발행하는 Flow를 생성합니다.
 *
 * ```
 * flowRangeOf(0, 10)
 *     .interval(200.milliseconds, 100.milliseconds)
 *     .assertResult(flowRangeOf(0, 10))
 * ```
 */
fun <T> Flow<T>.interval(
    initialDelay: Duration,
    delay: Duration = Duration.ZERO,
): Flow<T> = flow {
    val initialDelayValue = initialDelay.coerceAtLeast(Duration.ZERO)
    val delayValue = delay.coerceAtLeast(Duration.ZERO)

    if (initialDelayValue.isPositive()) {
        delay(initialDelayValue)
    }

    if (delayValue.isPositive()) {
        this@interval.collect { value ->
            delay(delayValue)
            emit(value)
        }
    } else {
        this@interval.collect { value ->
            emit(value)
        }
    }
}

/**
 * Flow를 주어진 [initialDelayMillis] 이후에 주어진 [delayMillis] 간격으로 발행하는 Flow를 생성합니다.
 *
 * ```
 * flowRangeOf(0, 10).log("source")
 *     .interval(100, 100).log("interval")
 *     .assertResult(flowRangeOf(0, 10))
 * ```
 */
fun <T> Flow<T>.interval(
    initialDelayMillis: Long = 0L,
    delayMillis: Long = 0L,
): Flow<T> = flow {
    val initialDelayValue = initialDelayMillis.coerceAtLeast(0L)
    val delayValue = delayMillis.coerceAtLeast(0L)

    if (initialDelayValue > 0L) {
        delay(initialDelayValue)
    }

    if (delayValue > 0L) {
        this@interval.collect { value ->
            delay(delayValue)
            emit(value)
        }
    } else {
        this@interval.collect { value ->
            emit(value)
        }
    }
}

/**
 * Flow를 주어진 [initialDelay] 이후에 주어진 [delay] 간격으로 발행하는 Flow를 생성합니다.
 *
 * ```
 * intervalFlowOf(200.milliseconds, 100.milliseconds)
 *     .take(20)
 *     .assertResult(flowRangeOf(0L, 20))
 * ```
 */
fun intervalFlowOf(initialDelay: Duration, delay: Duration): Flow<Long> = flow {
    val initialDelayValue = initialDelay.coerceAtLeast(Duration.ZERO)
    val delayValue = delay.coerceAtLeast(Duration.ZERO)

    delay(initialDelayValue)
    var sequencer = 0L
    while (true) {
        emit(sequencer++)
        delay(delayValue)
    }
}

/**
 * Flow를 주어진 [initialDelayMillis] 이후에 주어진 [delayMillis] 간격으로 발행하는 Flow를 생성합니다.
 *
 * ```
 * intervalFlowOf(200, 100)
 *     .take(20)
 *     .assertResult(flowRangeOf(0L, 20))
 * ```
 */
fun intervalFlowOf(initialDelayMillis: Long, delayMillis: Long): Flow<Long> = flow {
    val initialDelayValue = initialDelayMillis.coerceAtLeast(0L)
    val delayValue = delayMillis.coerceAtLeast(0L)

    delay(initialDelayValue)
    var sequencer = 0L
    while (true) {
        emit(sequencer++)
        delay(delayValue)
    }
}
