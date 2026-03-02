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
 * notifier가 첫 값을 방출할 때까지 원본 값을 건너뛰고 이후 값만 방출합니다.
 *
 * ## 동작/계약
 * - notifier의 첫 이벤트 전까지 source 값은 모두 drop됩니다.
 * - notifier가 첫 이벤트를 방출하면 게이트가 열리고 이후 source 값은 그대로 통과합니다.
 * - notifier가 아무 값 없이 완료되면 게이트가 열리지 않아 결과는 빈 Flow가 될 수 있습니다.
 * - source/notifier 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .onEach { delay(100) }
 *     .skipUntil(delayedFlow(150))
 *     .toList()
 * // out == [2, 3]
 * ```
 * @param notifier 게이트 오픈 시점을 알리는 Flow입니다.
 */
fun <T> Flow<T>.skipUntil(notifier: Flow<Any?>): Flow<T> = skipUntilInternal(this, notifier)

/**
 * 지정한 [Duration] 이후부터 값을 통과시키는 [skipUntil] 별칭입니다.
 *
 * ## 동작/계약
 * - 내부적으로 [delayedFlow]를 notifier로 사용합니다.
 * - [delay]가 0 이하이면 즉시 게이트가 열려 원본을 그대로 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).skipUntil(150.milliseconds).toList()
 * // out == [2, 3]
 * ```
 * @param delay 게이트 오픈 지연 시간입니다.
 */
fun <T> Flow<T>.skipUntil(delay: Duration): Flow<T> = skipUntil(delayedFlow(delay))

/**
 * 지정한 밀리초 이후부터 값을 통과시키는 [skipUntil] 별칭입니다.
 *
 * ## 동작/계약
 * - 내부적으로 [delayedFlow]를 notifier로 사용합니다.
 * - [delayMillis]가 0 이하이면 즉시 게이트가 열려 원본을 그대로 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).skipUntil(150L).toList()
 * // out == [2, 3]
 * ```
 * @param delayMillis 게이트 오픈 지연 시간(밀리초)입니다.
 */
fun <T> Flow<T>.skipUntil(delayMillis: Long): Flow<T> = skipUntil(delayedFlow(delayMillis))

/**
 * [skipUntil]의 의미상 별칭입니다.
 *
 * ## 동작/계약
 * - 동작은 [skipUntil](`notifier`)와 동일합니다.
 * - notifier 첫 이벤트 전까지 source 값을 drop합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .dropUntil(delayedFlow(150))
 *     .toList()
 * // out == [2, 3]
 * ```
 * @param notifier 게이트 오픈 시점을 알리는 Flow입니다.
 */
fun <T> Flow<T>.dropUntil(notifier: Flow<Any?>): Flow<T> = skipUntil(notifier)

/**
 * [skipUntil] Duration 오버로드의 의미상 별칭입니다.
 *
 * ## 동작/계약
 * - 동작은 [skipUntil](`Duration`)과 동일합니다.
 * - 지연 후 게이트가 열리고 이후 값만 통과합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).dropUntil(150.milliseconds).toList()
 * // out == [2, 3]
 * ```
 * @param delay 게이트 오픈 지연 시간입니다.
 */
fun <T> Flow<T>.dropUntil(delay: Duration): Flow<T> = skipUntil(delayedFlow(delay))

/**
 * [skipUntil] 밀리초 오버로드의 의미상 별칭입니다.
 *
 * ## 동작/계약
 * - 동작은 [skipUntil](`Long`)과 동일합니다.
 * - 지연 후 게이트가 열리고 이후 값만 통과합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).dropUntil(150L).toList()
 * // out == [2, 3]
 * ```
 * @param delayMillis 게이트 오픈 지연 시간(밀리초)입니다.
 */
fun <T> Flow<T>.dropUntil(delayMillis: Long): Flow<T> = skipUntil(delayedFlow(delayMillis))

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

private class SkipUntilState {
    val gate = atomic(false)
}
