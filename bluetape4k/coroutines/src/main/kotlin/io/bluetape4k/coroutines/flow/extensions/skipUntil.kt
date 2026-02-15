package io.bluetape4k.coroutines.flow.extensions

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * 두 번째 [Flow]([notifier])이 값을 방출하거나 완료될 때까지 소스 [Flow]에서 방출된 항목을 건너뛰는 [Flow]을 반환합니다.
 *
 * ```
 * // -------1-------2-------3
 * // -----------|
 * flowOf(1, 2, 3)
 *     .onEach { delay(100) }
 *     .skipUntil(delayedFlow(150))
 *     .assertResult(2, 3)
 * ```
 *
 * @param notifier 소스 [Flow]의 흐름을 제어하는 [Flow]
 */
fun <T> Flow<T>.skipUntil(notifier: Flow<Any?>): Flow<T> = skipUntilInternal(this, notifier)

/**
 * [delay] 동안 소스 [Flow]에서 방출된 항목을 건너뛰는 [Flow]를 반환합니다.
 *
 * @param delay 건너뛰기 할 시간
 */
fun <T> Flow<T>.skipUntil(delay: Duration): Flow<T> = skipUntil(delayedFlow(delay))

/**
 * [delayMillis] 동안 소스 [Flow]에서 방출된 항목을 건너뛰는 [Flow]를 반환합니다.
 *
 * ```
 * // 01-------------2X
 * // -------100
 * val source = flow {
 *    emit(0)
 *    emit(1)
 *    delay(20)
 *    emit(2)
 *    throw RuntimeException("Boom!")
 * }
 * val notifier = flowOf(100).onEach { delay(10) }.log("notifier")
 * source.skipUntil(20.milliseconds)
 *  .test {
 *    awaitItem() shouldBeEqualTo 2
 *    awaitError() shouldBeInstanceOf RuntimeException::class
 *   }
 * ```
 *
 * @param delayMillis 건너뛰기 할 시간 (millis seconds)
 */
fun <T> Flow<T>.skipUntil(delayMillis: Long): Flow<T> = skipUntil(delayedFlow(delayMillis))

/**
 * [skipUntil] 과 같은 기능을 하는 operator
 *
 * ```
 * // 01-------------2X
 * // -------100
 * val source = flow {
 *     emit(0)
 *     emit(1)
 *     delay(20)
 *     emit(2)
 *     throw RuntimeException("Boom!")
 * }
 * val notifier = flowOf(100).onEach { delay(10) }.log("notifier")
 * source
 *     .log("source")
 *     .buffer()
 *     .skipUntil(notifier)
 *     .onEach {
 *         it shouldBeEqualTo 2
 *     }
 *     .test {
 *         awaitItem() shouldBeEqualTo 2
 *         awaitError() shouldBeInstanceOf RuntimeException::class
 *     }
 * ```
 *
 * @param notifier 소스 [Flow]의 흐름을 제어하는 [Flow]
 */
fun <T> Flow<T>.dropUntil(notifier: Flow<Any?>): Flow<T> = skipUntil(notifier)

/**
 * [delay] 동안 소스 [Flow]에서 방출된 항목을 건너뛰는 [Flow]를 반환합니다.
 *
 * @param delay 건너뛰기 할 시간
 */
fun <T> Flow<T>.dropUntil(delay: Duration): Flow<T> = skipUntil(delayedFlow(delay))

/**
 * [delayMillis] 동안 소스 [Flow]에서 방출된 항목을 건너뛰는 [Flow]를 반환합니다.
 *
 * @param delayMillis 건너뛰기 할 시간 (millis seconds)
 */
fun <T> Flow<T>.dropUntil(delayMillis: Long): Flow<T> = skipUntil(delayedFlow(delayMillis))

/**
 * notifier에서 첫 요소를 수신한 뒤부터 [source] 요소를 전달합니다.
 */
internal fun <T> skipUntilInternal(source: Flow<T>, notifier: Flow<Any?>): Flow<T> = flow {
    coroutineScope {
        val state = SkipUntilState()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            notifier.take(1).collect()
            state.gate.value = true
        }

        source.collect {
            if (state.gate.value) {
                emit(it)
            }
        }

        job.cancel()
    }
}

/**
 * [skipUntilInternal]의 게이트 상태를 보관합니다.
 */
private class SkipUntilState {
    val gate = atomic(false)
}
