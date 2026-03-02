package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * 원본 Flow를 무한 반복합니다.
 *
 * ## 동작/계약
 * - 원본 Flow를 끝까지 수집한 뒤 즉시 다시 수집합니다.
 * - 종료 조건이 없어 외부에서 `take`, 취소 등으로 중단해야 합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).repeat().take(6).toList()
 * // out == [1, 2, 3, 1, 2, 3]
 * ```
 */
fun <T> Flow<T>.repeat(): Flow<T> =
    repeatInternal(this, 0, true, noDelayFunction())

/**
 * 원본 Flow를 무한 반복하되 각 반복 사이에 고정 지연을 둡니다.
 *
 * ## 동작/계약
 * - 반복 1회 완료 후 [duration]만큼 지연한 뒤 다음 반복을 시작합니다.
 * - [duration]이 0 이하이면 지연 없이 반복합니다.
 * - 종료 조건은 없으므로 외부에서 중단해야 합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).repeat(1.seconds).take(4).toList()
 * // out == [1, 2, 1, 2]
 * ```
 * @param duration 반복 사이 고정 지연 시간입니다.
 */
fun <T> Flow<T>.repeat(duration: Duration): Flow<T> =
    repeatInternal(this, 0, true, fixedDelayFunction(duration))

/**
 * 원본 Flow를 무한 반복하되 반복 인덱스별 지연 함수를 사용합니다.
 *
 * ## 동작/계약
 * - 반복 1회 완료 후 [durationFunc] 결과만큼 지연합니다.
 * - 지연 함수 예외는 수집 시점에 전파됩니다.
 * - 종료 조건은 없으므로 외부에서 중단해야 합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).repeat { (it + 1).milliseconds }.take(4).toList()
 * // out == [1, 2, 1, 2]
 * ```
 * @param durationFunc 반복 인덱스 기반 지연 함수입니다.
 */
fun <T> Flow<T>.repeat(durationFunc: suspend (index: Int) -> Duration): Flow<T> =
    repeatInternal(this, 0, true, delayFunction(durationFunc))


/**
 * 원본 Flow를 최대 [count]회 반복합니다.
 *
 * ## 동작/계약
 * - [count]가 0 이하이면 빈 Flow를 반환합니다.
 * - [count]가 양수면 정확히 [count]회 반복 수집합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).repeat(2).toList()
 * // out == [1, 2, 3, 1, 2, 3]
 * ```
 * @param count 반복 횟수입니다.
 */
fun <T> Flow<T>.repeat(count: Int): Flow<T> {
    return repeatInternal(this, count, false, noDelayFunction())
}

/**
 * 원본 Flow를 최대 [count]회 반복하고 반복 사이에 고정 지연을 둡니다.
 *
 * ## 동작/계약
 * - [count]가 0 이하이면 빈 Flow를 반환합니다.
 * - 각 반복 뒤 [duration]만큼 지연합니다.
 * - [duration]이 0 이하이면 지연 없이 반복합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).repeat(2, 100.milliseconds).toList()
 * // out == [1, 2, 1, 2]
 * ```
 * @param count 반복 횟수입니다.
 * @param duration 반복 사이 고정 지연 시간입니다.
 */
fun <T> Flow<T>.repeat(count: Int, duration: Duration): Flow<T> {
    return repeatInternal(this, count, false, fixedDelayFunction(duration))
}

/**
 * 원본 Flow를 최대 [count]회 반복하고 반복 인덱스별 지연 함수를 사용합니다.
 *
 * ## 동작/계약
 * - [count]가 0 이하이면 빈 Flow를 반환합니다.
 * - 각 반복 뒤 [durationFunc] 결과만큼 지연합니다.
 * - 지연 함수 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).repeat(2) { (it + 1).milliseconds }.toList()
 * // out == [1, 2, 1, 2]
 * ```
 * @param count 반복 횟수입니다.
 * @param durationFunc 반복 인덱스 기반 지연 함수입니다.
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
    infinite -> repeatIndefinitely(flow, durationFunc)
    count <= 0 -> emptyFlow()
    else     -> repeatAtMostCount(flow, count, durationFunc)
}

private fun <T> repeatIndefinitely(
    flow: Flow<T>,
    durationFunc: DelayDurationFunction?,
): Flow<T> = when (durationFunc) {
    null -> flow {
        while (true) {
            emitAll(flow)
        }
    }

    is FixedDelayDurationFunction -> flow {
        while (true) {
            emitAll(flow)
            delay(durationFunc.duration)
        }
    }

    else -> flow {
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
): Flow<T> = flow {
    repeat(count) {
        emitAll(flow)
        durationFunc?.let { func ->
            delay(func(it))
        }
    }
}
