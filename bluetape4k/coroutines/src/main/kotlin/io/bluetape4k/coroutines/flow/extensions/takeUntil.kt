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
 * [other] 소스에서 요소가 emit 되거나 완료될 때까지는 main source로부터 소비한다.
 *
 * ```
 * flowRangeOf(1, 5)
 *     .takeUntil(flowOf(1))
 *     .assertResult()
 * ```
 * @param other 소스 [Flow]의 흐름을 제어하는 [Flow]
 * @return Flow<T> 인스턴스
 */
fun <T> Flow<T>.takeUntil(other: Flow<Any?>): Flow<T> = takeUntilInternal(this, other)

/**
 * [delay] 만큼 지연해서 flow 를 collect 하도록 합니다.
 *
 * ```
 * flowRangeOf(1, 10)
 *     .onEach { delay(100) }
 *     .takeUntil(550.milliseconds)
 *     .assertResult(1, 2, 3, 4, 5)
 * ```
 *
 * @param delay 지연할 시간
 * @return Flow<T> 인스턴스
 */
fun <T> Flow<T>.takeUntil(delay: Duration): Flow<T> = takeUntilInternal(this, delayedFlow(delay))

/**
 * [delayMillis] 만큼 지연해서 flow 를 collect 하도록 합니다.
 *
 * ```
 * flowRangeOf(1, 10)
 *     .onEach { delay(100) }
 *     .takeUntil(550)
 *     .assertResult(1, 2, 3, 4, 5)
 * ```
 *
 * @param delayMillis 지연할 시간 (단위: MilliSeconds)
 * @return Flow<T> 인스턴스
 */
fun <T> Flow<T>.takeUntil(delayMillis: Long): Flow<T> = takeUntilInternal(this, delayedFlow(delayMillis))


internal fun <T> takeUntilInternal(source: Flow<T>, notifier: Flow<Any?>): Flow<T> = flow {
    coroutineScope {
        val gate = atomic(false)

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                notifier.collect {
                    throw STOP
                }
            } catch (e: StopFlowException) {
                // Nothing to do
            } finally {
                gate.value = true
            }
        }

        try {
            source.collect {
                if (gate.value) {
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
