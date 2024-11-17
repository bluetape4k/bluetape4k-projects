package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

/**
 * Flow 를 반복합니다.
 *
 * ```
 * flowOf(1,2,3).repeat().take(6) // 1,2,3,1,2,3
 * ```
 */
fun <T> Flow<T>.repeat(): Flow<T> =
    repeatInternal(this, 0, true, noDelayFunction())

/**
 * Flow를 [duration] 기간 동안 반복합니다.
 *
 * ```
 * flowOf(1,2,3).repeat(1.seconds).take(6) // 1,2,3,1,2,3
 * ```
 */
fun <T> Flow<T>.repeat(duration: Duration): Flow<T> =
    repeatInternal(this, 0, true, fixedDelayFunction(duration))

/**
 * Flow를 [durationFunc] 함수의 반환값 기간 동안 반복합니다.
 *
 * ```
 * flowOf(1,2,3).repeat { 1.seconds }.take(6) // 1,2,3,1,2,3
 * ```
 */
fun <T> Flow<T>.repeat(durationFunc: suspend (index: Int) -> Duration): Flow<T> =
    repeatInternal(this, 0, true, delayFunction(durationFunc))


/**
 * Flow를 [count] 만큼 반복합니다.
 *
 * ```
 * flowOf(1,2,3).repeat(2) // 1,2,3,1,2,3
 * ```
 */
fun <T> Flow<T>.repeat(count: Int): Flow<T> {
    return repeatInternal(this, count, false, noDelayFunction())
}

/**
 * Flow를 [count] 만큼 [duration] 기간 동안 반복합니다.
 *
 * ```
 * flowOf(1,2,3).repeat(2, 1.seconds) // 1,2,3,1,2,3
 * ```
 */
fun <T> Flow<T>.repeat(count: Int, duration: Duration): Flow<T> {
    return repeatInternal(this, count, false, fixedDelayFunction(duration))
}

/**
 * Flow를 [count] 만큼 [durationFunc] 함수의 반환값 기간 동안 반복합니다.
 *
 * ```
 * flowOf(1,2,3).repeat(2) { 1.seconds } // 1,2,3,1,2,3
 * ```
 */
fun <T> Flow<T>.repeat(count: Int, durationFunc: suspend (index: Int) -> Duration): Flow<T> {
    return repeatInternal(this, count, false, delayFunction(durationFunc))
}

// --------------- INTERNAL -------------------

private typealias DelayDurationFunction = suspend (count: Int) -> Duration

private fun noDelayFunction(): DelayDurationFunction? = null
private fun fixedDelayFunction(duration: Duration): DelayDurationFunction? {
    return if (duration.isZeroOrNegative()) {
        noDelayFunction()
    } else {
        FixedDelayDurationFunction(duration)
    }
}

private fun delayFunction(durationFunc: DelayDurationFunction): DelayDurationFunction = durationFunc

private fun Duration.isZeroOrNegative(): Boolean =
    this == Duration.ZERO || isNegative()

private class FixedDelayDurationFunction(val duration: Duration): DelayDurationFunction {
    override suspend fun invoke(count: Int): Duration = duration
}

private fun <T> repeatInternal(
    flow: Flow<T>,
    count: Int,
    infinite: Boolean,
    durationFunc: DelayDurationFunction?,
): Flow<T> = when {
    infinite   -> repeatIndefinitely(flow, durationFunc)
    count <= 0 -> emptyFlow()
    else       -> repeatAtMostCount(flow, count, durationFunc)
}

private fun <T> repeatIndefinitely(
    flow: Flow<T>,
    durationFunc: DelayDurationFunction?,
): Flow<T> = when (durationFunc) {
    null                          ->
        kotlinx.coroutines.flow.flow {
            while (true) {
                emitAll(flow)
            }
        }

    is FixedDelayDurationFunction ->
        kotlinx.coroutines.flow.flow {
            while (true) {
                emitAll(flow)
                delay(durationFunc.duration)
            }
        }

    else                          ->
        kotlinx.coroutines.flow.flow {
            var soFar = 1
            while (true) {
                emitAll(flow)
                delay(durationFunc(soFar++))
            }
        }
}

private fun <T> repeatAtMostCount(
    flow: Flow<T>,
    count: Int,
    durationFunc: DelayDurationFunction?,
): Flow<T> = when (durationFunc) {
    null                          ->
        kotlinx.coroutines.flow.flow {
            repeat(count) {
                emitAll(flow)
            }
        }

    is FixedDelayDurationFunction ->
        kotlinx.coroutines.flow.flow {
            repeat(count) {
                emitAll(flow)
                delay(durationFunc.duration)
            }
        }

    else                          ->
        kotlinx.coroutines.flow.flow {
            repeat(count) {
                emitAll(flow)
                delay(durationFunc(it))
            }
        }
}
