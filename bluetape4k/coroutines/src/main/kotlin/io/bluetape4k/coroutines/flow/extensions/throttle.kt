package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.FlowOperationException
import io.bluetape4k.coroutines.flow.extensions.utils.DONE_VALUE
import io.bluetape4k.coroutines.flow.extensions.utils.NULL_VALUE
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * throttle 윈도우에서 선행/후행 방출 정책을 정의합니다.
 *
 * ## 동작/계약
 * - `LEADING`은 윈도우의 첫 요소만 방출합니다.
 * - `TRAILING`은 윈도우의 마지막 요소만 방출합니다.
 * - `BOTH`는 첫 요소와 마지막 요소를 모두 방출합니다.
 * - 정책 자체는 상태를 갖지 않으며 enum 상수 비교만 수행합니다.
 *
 * ```kotlin
 * val behavior = ThrottleBehavior.BOTH
 * // behavior == ThrottleBehavior.BOTH
 * ```
 */
enum class ThrottleBehavior {
    /** 각 윈도우에서 가장 먼저 관측된 요소만 방출합니다. */
    LEADING,

    /** 각 윈도우에서 마지막으로 관측된 요소만 방출합니다. */
    TRAILING,

    /** 각 윈도우에서 선행 요소와 후행 요소를 모두 방출합니다. */
    BOTH
}

/**
 * 현재 정책이 선행 방출을 포함하는지 반환합니다.
 *
 * ## 동작/계약
 * - `LEADING`, `BOTH`이면 `true`를 반환합니다.
 * - `TRAILING`이면 `false`를 반환합니다.
 * - 수신 enum 값을 변경하지 않습니다.
 *
 * ```kotlin
 * val value = ThrottleBehavior.BOTH.isLeading
 * // value == true
 * ```
 */
val ThrottleBehavior.isLeading: Boolean
    get() = this == ThrottleBehavior.LEADING || this == ThrottleBehavior.BOTH

/**
 * 현재 정책이 후행 방출을 포함하는지 반환합니다.
 *
 * ## 동작/계약
 * - `TRAILING`, `BOTH`이면 `true`를 반환합니다.
 * - `LEADING`이면 `false`를 반환합니다.
 * - 수신 enum 값을 변경하지 않습니다.
 *
 * ```kotlin
 * val value = ThrottleBehavior.LEADING.isTrailing
 * // value == false
 * ```
 */
val ThrottleBehavior.isTrailing: Boolean
    get() = this == ThrottleBehavior.TRAILING || this == ThrottleBehavior.BOTH


/**
 * 고정 [duration] 기준으로 선행 요소만 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 윈도우의 첫 요소를 즉시 방출하고, 윈도우가 닫힐 때까지 나머지 요소를 무시합니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 * - [duration]이 `Duration.ZERO`면 입력 요소를 즉시 통과시킵니다.
 * - 업스트림/다운스트림/시간 계산 중 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }.throttleLeading(501.milliseconds).collect { result += it }
 * // result == [1, 4, 7, 10]
 * ```
 * @param duration 윈도우 길이입니다.
 */
fun <T> Flow<T>.throttleLeading(duration: Duration): Flow<T> =
    throttleTime(ThrottleBehavior.LEADING) { duration }

/**
 * 밀리초 단위 윈도우로 선행 요소만 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 동작은 [throttleLeading]과 동일하며 시간 단위만 `Long` 밀리초를 사용합니다.
 * - `0L`이면 입력 요소를 즉시 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }.throttleLeading(501L).collect { result += it }
 * // result == [1, 4, 7, 10]
 * ```
 * @param timeMillis 윈도우 길이(밀리초)입니다.
 */
fun <T> Flow<T>.throttleLeading(timeMillis: Long): Flow<T> =
    throttleTime(ThrottleBehavior.LEADING) { timeMillis.milliseconds }

/**
 * 요소별 윈도우 길이로 선행 요소만 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 요소마다 [durationSelector]로 계산한 윈도우 길이를 사용합니다.
 * - 계산된 길이가 `Duration.ZERO`면 해당 요소 이후 윈도우를 즉시 닫습니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 * - [durationSelector]에서 발생한 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }.throttleLeading { 501.milliseconds }.collect { result += it }
 * // result == [1, 4, 7, 10]
 * ```
 * @param durationSelector 요소별 윈도우 길이를 계산하는 함수입니다.
 */
fun <T> Flow<T>.throttleLeading(durationSelector: (value: T) -> Duration): Flow<T> =
    throttleTime(ThrottleBehavior.LEADING, durationSelector)

/**
 * 밀리초 단위 윈도우로 후행 요소만 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 윈도우의 마지막 요소를 윈도우 종료 시점에 방출합니다.
 * - `0L`이면 입력 요소를 즉시 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }.throttleTrailing(501L).collect { result += it }
 * // result == [3, 6, 9, 10]
 * ```
 * @param timeMillis 윈도우 길이(밀리초)입니다.
 */
fun <T> Flow<T>.throttleTrailing(timeMillis: Long): Flow<T> =
    throttleTime(ThrottleBehavior.TRAILING) { timeMillis.milliseconds }

/**
 * 고정 [duration] 기준으로 후행 요소만 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 윈도우의 마지막 요소를 윈도우 종료 시점에 방출합니다.
 * - [duration]이 `Duration.ZERO`면 입력 요소를 즉시 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }.throttleTrailing(501.milliseconds).collect { result += it }
 * // result == [3, 6, 9, 10]
 * ```
 * @param duration 윈도우 길이입니다.
 */
fun <T> Flow<T>.throttleTrailing(duration: Duration): Flow<T> =
    throttleTime(ThrottleBehavior.TRAILING) { duration }

/**
 * 요소별 윈도우 길이로 후행 요소만 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 요소마다 [durationSelector]로 계산한 윈도우 길이를 사용합니다.
 * - 계산된 길이가 `Duration.ZERO`면 해당 요소를 즉시 후행 값으로 방출합니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 * - [durationSelector]에서 발생한 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }.throttleTrailing { 501.milliseconds }.collect { result += it }
 * // result == [3, 6, 9, 10]
 * ```
 * @param durationSelector 요소별 윈도우 길이를 계산하는 함수입니다.
 */
fun <T> Flow<T>.throttleTrailing(durationSelector: (value: T) -> Duration): Flow<T> =
    throttleTime(ThrottleBehavior.TRAILING, durationSelector)

/**
 * 밀리초 단위 윈도우에서 선행/후행 요소를 모두 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 윈도우의 첫 요소와 마지막 요소를 방출합니다.
 * - `0L`이면 입력 요소를 즉시 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowOf(1, 2, 3).throttleBoth(0L).collect { result += it }
 * // result == [1, 2, 3]
 * ```
 * @param timeMillis 윈도우 길이(밀리초)입니다.
 */
fun <T> Flow<T>.throttleBoth(timeMillis: Long): Flow<T> =
    throttleTime(ThrottleBehavior.BOTH) { timeMillis.milliseconds }

/**
 * 고정 [duration] 기준으로 선행/후행 요소를 모두 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 윈도우의 첫 요소와 마지막 요소를 방출합니다.
 * - [duration]이 `Duration.ZERO`면 입력 요소를 즉시 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowOf(1, 2, 3).throttleBoth(Duration.ZERO).collect { result += it }
 * // result == [1, 2, 3]
 * ```
 * @param duration 윈도우 길이입니다.
 */
fun <T> Flow<T>.throttleBoth(duration: Duration): Flow<T> =
    throttleTime(ThrottleBehavior.BOTH) { duration }

/**
 * 요소별 윈도우 길이로 선행/후행 요소를 모두 방출하는 throttle 연산을 적용합니다.
 *
 * ## 동작/계약
 * - 각 요소마다 [durationSelector]로 계산한 윈도우 길이를 사용합니다.
 * - 계산된 길이가 `Duration.ZERO`면 즉시 윈도우를 닫아 연속 값을 빠르게 통과시킵니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 * - [durationSelector]에서 발생한 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowOf(1, 2, 3).throttleBoth { Duration.ZERO }.collect { result += it }
 * // result == [1, 2, 3]
 * ```
 * @param durationSelector 요소별 윈도우 길이를 계산하는 함수입니다.
 */
fun <T> Flow<T>.throttleBoth(durationSelector: (value: T) -> Duration): Flow<T> =
    throttleTime(ThrottleBehavior.BOTH, durationSelector)


/**
 * 고정 [duration]으로 throttle 정책을 적용합니다.
 *
 * ## 동작/계약
 * - 실제 동작은 [throttleTime](`durationSelector`) 오버로드로 위임됩니다.
 * - [throttleBehavior]에 따라 선행/후행 방출 여부가 결정됩니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }
 *     .throttleTime(501.milliseconds, ThrottleBehavior.LEADING)
 *     .collect { result += it }
 * // result == [1, 4, 7, 10]
 * ```
 * @param duration 윈도우 길이입니다.
 * @param throttleBehavior 방출 정책입니다.
 */
fun <T> Flow<T>.throttleTime(
    duration: Duration,
    throttleBehavior: ThrottleBehavior = ThrottleBehavior.LEADING,
): Flow<T> =
    throttleTime(throttleBehavior) { duration }


/**
 * 밀리초 단위 시간으로 throttle 정책을 적용합니다.
 *
 * ## 동작/계약
 * - 동작은 [throttleTime](`Duration`, `ThrottleBehavior`)와 동일합니다.
 * - [timeMillis]를 [Duration]으로 변환해 처리합니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowRangeOf(1, 10).onEach { delay(200) }
 *     .throttleTime(501L, ThrottleBehavior.TRAILING)
 *     .collect { result += it }
 * // result == [3, 6, 9, 10]
 * ```
 * @param timeMillis 윈도우 길이(밀리초)입니다.
 * @param throttleBehavior 방출 정책입니다.
 */
fun <T> Flow<T>.throttleTime(
    timeMillis: Long,
    throttleBehavior: ThrottleBehavior = ThrottleBehavior.LEADING,
): Flow<T> =
    throttleTime(throttleBehavior) { timeMillis.milliseconds }


/**
 * 요소별 동적 윈도우 길이로 throttle 정책을 적용합니다.
 *
 * ## 동작/계약
 * - 내부 상태(`lastValue`, `throttled`)를 사용해 윈도우 중복 방출을 제어합니다.
 * - [durationSelector]가 `Duration.ZERO`를 반환하면 즉시 윈도우를 닫고 후행 처리로 진행합니다.
 * - 수신 Flow를 변경하지 않고 새 Flow를 반환하며, nullable 요소는 내부 sentinel로 안전하게 처리합니다.
 * - 업스트림 채널 종료 원인이 예외면 [FlowOperationException]으로 감싸 전파합니다.
 *
 * ```kotlin
 * val result = mutableListOf<Int>()
 * flowOf(1, 2, 3).throttleTime(ThrottleBehavior.BOTH) { Duration.ZERO }.collect { result += it }
 * // result == [1, 2, 3]
 * ```
 * @param throttleBehavior 윈도우에서 선행/후행 중 어떤 값을 방출할지 지정합니다.
 * @param durationSelector 요소별 윈도우 길이를 계산합니다.
 */
fun <T> Flow<T>.throttleTime(
    throttleBehavior: ThrottleBehavior = ThrottleBehavior.LEADING,
    durationSelector: (value: T) -> Duration,
): Flow<T> = flow {
    val leading = throttleBehavior.isLeading
    val trailing = throttleBehavior.isTrailing
    val downstream = this

    coroutineScope {
        val scope = this

        // Produce the values using the default (rendezvous) channel
        val values: ReceiveChannel<Any> = produce {
            collect { value ->
                send(value ?: NULL_VALUE)
            }
        }

        var lastValue: Any? = null
        var throttled: Job? = null

        suspend fun trySend() {
            lastValue?.let { consumed ->
                check(lastValue !== DONE_VALUE)

                // Ensure we clear out our lastValue
                // before we emit, otherwise reentrant code can cause
                // issues here.
                lastValue = null // Consume the value
                return@let downstream.emit(NULL_VALUE.unbox(consumed))
            }
        }

        val onWindowClosed = suspend {
            throttled = null
            if (trailing) {
                trySend()
            }
        }

        // Now consume the values until the original flow is complete.
        while (lastValue !== DONE_VALUE) {
            kotlinx.coroutines.selects.select<Unit> {
                // When a throttling window ends, send the value if there is a pending value.
                throttled?.onJoin?.invoke(onWindowClosed)

                values.onReceiveCatching { result: ChannelResult<Any> ->
                    result
                        .onSuccess { value: Any ->
                            lastValue = value

                            // If we are not within a throttling window, immediately send the value (if leading is true)
                            // and then start throttling.

                            throttled?.let { return@onSuccess }
                            if (leading) {
                                trySend()
                            }
                            when (val duration = durationSelector(NULL_VALUE.unbox(value))) {
                                Duration.ZERO -> onWindowClosed()
                                else          -> throttled = scope.launch { delay(duration) }
                            }
                        }
                        .onFailure { error ->
                            error?.let { throw FlowOperationException("Fail to throttling", error) }

                            // Once the original flow has completed, there may still be a pending value
                            // waiting to be emitted. If so, wait for the throttling window to end and then
                            // send it. That will complete this throttled flow.
                            if (trailing && lastValue != null) {
                                throttled?.run {
                                    throttled = null
                                    join()
                                    trySend()
                                }
                            }

                            lastValue = DONE_VALUE
                        }
                }
            }
        }

        throttled?.run {
            // throttled = null
            cancelAndJoin()
        }
    }
}
