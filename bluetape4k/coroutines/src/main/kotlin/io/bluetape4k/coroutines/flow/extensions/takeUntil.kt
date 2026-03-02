package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.STOP
import io.bluetape4k.coroutines.flow.exceptions.StopFlowException
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * 다른 Flow가 값을 내보내는 시점까지 수신 Flow를 방출합니다.
 *
 * ## 동작/계약
 * - `other`가 첫 값을 방출하면 즉시 중단 신호를 보내고 수신 Flow 방출을 종료합니다.
 * - `other`가 아무 값도 없이 종료되면 수신 Flow 전체를 그대로 통과시킵니다.
 * - 내부적으로 notifier 수집용 코루틴 1개를 추가로 실행합니다.
 *
 * ```kotlin
 * val source = flowOf(1, 2, 3)
 * val stop = flowOf(Unit)
 * val result = source.takeUntil(stop).toList()
 * // result == []
 * ```
 *
 * @param other 중단 트리거 역할을 하는 Flow입니다.
 */
fun <T> Flow<T>.takeUntil(other: Flow<Any?>): Flow<T> =
    takeUntilInternal(this, other)

/**
 * 지정한 시간 동안만 수신 Flow를 방출합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `delayedFlow(delay)`를 notifier로 사용한 `takeUntil`과 동일하게 동작합니다.
 * - 타이머가 먼저 도착하면 즉시 방출을 중단합니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).takeUntil(1.seconds).toList()
 * // 타이머 이전에 도착한 요소만 남는다.
 * ```
 *
 * @param delay 중단까지 대기할 시간입니다.
 */
fun <T> Flow<T>.takeUntil(delay: Duration): Flow<T> =
    takeUntilInternal(this, delayedFlow(delay))

/**
 * 지정한 밀리초 동안만 수신 Flow를 방출합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `delayedFlow(delayMillis)` notifier를 사용합니다.
 * - 음수/0 지연값 처리 규칙은 `delayedFlow` 구현 계약을 따릅니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).takeUntil(500L).toList()
 * // 500ms 이전에 도착한 요소만 남는다.
 * ```
 *
 * @param delayMillis 중단까지 대기할 밀리초입니다.
 */
fun <T> Flow<T>.takeUntil(delayMillis: Long): Flow<T> =
    takeUntilInternal(this, delayedFlow(delayMillis))

internal fun <T> takeUntilInternal(source: Flow<T>, notifier: Flow<Any?>): Flow<T> = flow {
    coroutineScope {
        val state = TakeUntilState()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                notifier.collect {
                    throw STOP
                }
            } catch (e: StopFlowException) {
                // Nothing to do
            } finally {
                state.gate.value = true
            }
        }

        try {
            source.collect {
                if (state.gate.value) {
                    throw STOP
                }
                emit(it)
            }
        } catch (e: StopFlowException) {
            // Nothing to do
        } finally {
            job.cancel(STOP)
        }
    }
}

private class TakeUntilState {
    val gate = atomic(false)
}
