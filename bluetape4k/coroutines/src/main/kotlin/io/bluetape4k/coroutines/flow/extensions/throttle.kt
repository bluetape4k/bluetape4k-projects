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
 * Define leading and trailing behavior.
 */
enum class ThrottleBehavior {
    /**
     * 각 윈도우의 첫번째 요소만 방출합니다.
     *
     * @see [kotlinx.coroutines.flow.debounce]
     * @see [kotlinx.coroutines.flow.sample]
     */
    LEADING,

    /**
     * 각 윈도우의 마지막 요소만 방출합니다.
     *
     * @see [kotlinx.coroutines.flow.debounce]
     * @see [kotlinx.coroutines.flow.sample]
     */
    TRAILING,

    /**
     * 각 윈도우의 첫번째와 마지막 요소를 모두 방출합니다.
     */
    BOTH
}

/**
 * [ThrottleBehavior] 가 윈도우의 첫번재 요소를 방출하는지 여부를 반환합니다.
 * [ThrottleBehavior.LEADING] 또는 [ThrottleBehavior.BOTH] 인 경우 `true`를 반환합니다.
 */
val ThrottleBehavior.isLeading: Boolean
    get() = this == ThrottleBehavior.LEADING || this == ThrottleBehavior.BOTH

/**
 * [ThrottleBehavior] 가 윈도우의 마지막 요소를 방출하는지 여부를 반환합니다.
 * [ThrottleBehavior.TRAILING] 또는 [ThrottleBehavior.BOTH] 인 경우 `true`를 반환합니다.
 */
val ThrottleBehavior.isTrailing: Boolean
    get() = this == ThrottleBehavior.TRAILING || this == ThrottleBehavior.BOTH


/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [duration] 윈도우의 선두 요소를 emit 합니다.
 *
 * ```
 * //-----1-----2-----3-----4-----5-----6-----7-----8-----9-----10
 * //--------------|--------------|----------------|-------------|
 * // 1 - deliver (200)
 * // 2 - skip    (400)
 * // ---------------------------- 501
 * // 3 - skip    (600)
 * // 4 - deliver (800)
 * // ---------------------------- 1002
 * // 5 - skip    (1000)
 * // 6 - skip    (1200)
 * // 7 - deliver (1400)
 * // ---------------------------- 1503
 * // 8 - skip    (1600)
 * // 9 - skip    (1800)
 * // 10 - deliver (2000)
 * // ---------------------------- 2004
 * flowRangeOf(1, 10)
 *     .onEach { delay(200.milliseconds) }
 *     .throttleLeading(501)
 *     .assertResult(1, 4, 7, 10)
 * ```
 */
fun <T> Flow<T>.throttleLeading(duration: Duration): Flow<T> =
    throttleTime(ThrottleBehavior.LEADING) { duration }

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [timeMillis] 윈도우의 선두 요소를 emit 합니다.
 *
 * ```
 * //-----1-----2-----3-----4-----5-----6-----7-----8-----9-----10
 * //--------------|--------------|----------------|-------------|
 * // 1 - deliver (200)
 * // 2 - skip    (400)
 * // ---------------------------- 501
 * // 3 - skip    (600)
 * // 4 - deliver (800)
 * // ---------------------------- 1002
 * // 5 - skip    (1000)
 * // 6 - skip    (1200)
 * // 7 - deliver (1400)
 * // ---------------------------- 1503
 * // 8 - skip    (1600)
 * // 9 - skip    (1800)
 * // 10 - deliver (2000)
 * // ---------------------------- 2004
 * flowRangeOf(1, 10)
 *     .onEach { delay(200) }
 *     .throttleLeading(501)
 *     .assertResult(1, 4, 7, 10)
 * ```
 */
fun <T> Flow<T>.throttleLeading(timeMillis: Long): Flow<T> =
    throttleTime(ThrottleBehavior.LEADING) { timeMillis.milliseconds }

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [durationSelector] 윈도우의 선두 요소를 emit 합니다.
 *
 * ```
 * flowRangeOf(1, 10)
 *     .onEach { delay(200) }
 *     .throttleLeading { Duration.ZERO }
 *     .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * ```
 */
fun <T> Flow<T>.throttleLeading(durationSelector: (value: T) -> Duration): Flow<T> =
    throttleTime(ThrottleBehavior.LEADING, durationSelector)

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [timeMillis] 윈도우의 마지막 요소를 emit 합니다.
 *
 * ```
 * // -1---2----3-
 * // -@-----!--@-----!
 * // -------2--------3
 * flow {
 *     delay(100)
 *     emit(1)
 *     delay(300)      // 400
 *     emit(2)
 *     delay(400)      // 800
 *     emit(3)
 *     delay(100)      // 900
 * }
 *     .throttleTrailing(500)
 *     .assertResult(2, 3)
 * ```
 */
fun <T> Flow<T>.throttleTrailing(timeMillis: Long): Flow<T> =
    throttleTime(ThrottleBehavior.TRAILING) { timeMillis.milliseconds }

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [duration] 윈도우의 마지막 요소를 emit 합니다.
 *
 * ```
 * // -1---2----3-
 * // -@-----!--@-----!
 * // -------2--------3
 * flow {
 *     delay(100)
 *     emit(1)
 *     delay(300)      // 400
 *     emit(2)
 *     delay(400)      // 800
 *     emit(3)
 *     delay(100)      // 900
 * }
 *     .throttleTrailing(500.milliseconds)
 *     .assertResult(2, 3)
 * ```
 */
fun <T> Flow<T>.throttleTrailing(duration: Duration): Flow<T> =
    throttleTime(ThrottleBehavior.TRAILING) { duration }

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [durationSelector] 윈도우의 마지막 요소를 emit 합니다.
 *
 * ```
 * // -1---2----3-
 * // -@-----!--@-----!
 * // -------2--------3
 * flow {
 *     delay(100)
 *     emit(1)
 *     delay(300)      // 400
 *     emit(2)
 *     delay(400)      // 800
 *     emit(3)
 *     delay(100)      // 900
 * }
 *     .throttleTrailing { 500.milliseconds }
 *     .assertResult(2, 3)
 * ```
 */
fun <T> Flow<T>.throttleTrailing(durationSelector: (value: T) -> Duration): Flow<T> =
    throttleTime(ThrottleBehavior.TRAILING, durationSelector)

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [timeMillis] 윈도우의 첫뻔째와 마지막 요소를 emit 합니다.
 *
 * ```
 * // -1---2----3-
 * // -@-----!--@-----!
 * // -------2--------3
 * flow {
 *     delay(100)
 *     emit(1)
 *     delay(300)      // 400
 *     emit(2)
 *     delay(400)      // 800
 *     emit(3)
 *     delay(100)      // 900
 * }
 *     .throttleBoth(500)
 *     .assertResult(1, 2, 3)
 * ```
 */
fun <T> Flow<T>.throttleBoth(timeMillis: Long): Flow<T> =
    throttleTime(ThrottleBehavior.BOTH) { timeMillis.milliseconds }

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [duration] 윈도우의 첫뻔째와 마지막 요소를 emit 합니다.
 *
 * ```
 * // -1---2----3-
 * // -@-----!--@-----!
 * // -------2--------3
 * flow {
 *     delay(100)
 *     emit(1)
 *     delay(300)      // 400
 *     emit(2)
 *     delay(400)      // 800
 *     emit(3)
 *     delay(100)      // 900
 * }
 *     .throttleBoth(500.milliseconds)
 *     .assertResult(1, 2, 3)
 * ```
 */
fun <T> Flow<T>.throttleBoth(duration: Duration): Flow<T> =
    throttleTime(ThrottleBehavior.BOTH) { duration }

/**
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [durationSelector] 윈도우의 첫뻔째와 마지막 요소를 emit 합니다.
 *
 * ```
 * // -1---2----3-
 * // -@-----!--@-----!
 * // -------2--------3
 * flow {
 *     delay(100)
 *     emit(1)
 *     delay(300)      // 400
 *     emit(2)
 *     delay(400)      // 800
 *     emit(3)
 *     delay(100)      // 900
 * }
 *     .throttleBoth { 500.milliseconds }
 *     .assertResult(1, 2, 3)
 * ```
 */
fun <T> Flow<T>.throttleBoth(durationSelector: (value: T) -> Duration): Flow<T> =
    throttleTime(ThrottleBehavior.BOTH, durationSelector)


/**
 * Source [Flow]에서 값을 방출하는 [Flow]를 반환한 다음,
 * [duration]기간 동안 후속 소스 값을 무시한 후, 방출한다.
 *
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [ThrottleBehavior]에 따라서 전, 후, 모두 값을 방출할 수 있다.
 *
 * * Example [ThrottleBehavior.LEADING]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(500.milliseconds)
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 1, 4, 7, 10
 * ```
 *
 * * Example [ThrottleBehavior.TRAILING]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(500.milliseconds, ThrottleBehavior.TRAILING)
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 3, 6, 9, 10
 * ```
 *
 * * Example [ThrottleBehavior.BOTH]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(500.milliseconds, ThrottleBehavior.BOTH)
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 1, 3, 4, 6, 7, 9, 10
 * ```
 */
fun <T> Flow<T>.throttleTime(
    duration: Duration,
    throttleBehavior: ThrottleBehavior = ThrottleBehavior.LEADING,
): Flow<T> =
    throttleTime(throttleBehavior) { duration }


/**
 * Source [Flow]에서 값을 방출하는 [Flow]를 반환한 다음,
 * [timeMillis] 기간 동안 후속 소스 값을 무시한 후, 방출한다.
 *
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [ThrottleBehavior]에 따라서 전, 후, 모두 값을 방출할 수 있다.
 *
 * * Example [ThrottleBehavior.LEADING]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(500.milliseconds)
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 1, 4, 7, 10
 * ```
 *
 * * Example [ThrottleBehavior.TRAILING]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(500.milliseconds, ThrottleBehavior.TRAILING)
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 3, 6, 9, 10
 * ```
 *
 * * Example [ThrottleBehavior.BOTH]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(500.milliseconds, ThrottleBehavior.BOTH)
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 1, 3, 4, 6, 7, 9, 10
 * ```
 */
fun <T> Flow<T>.throttleTime(
    timeMillis: Long,
    throttleBehavior: ThrottleBehavior = ThrottleBehavior.LEADING,
): Flow<T> =
    throttleTime(throttleBehavior) { timeMillis.milliseconds }


/**
 * Source [Flow]에서 값을 방출하는 [Flow]를 반환한 다음,
 * [durationSelector]가 지정한 기간 동안 후속 소스 값을 무시한 후, 방출한다.
 *
 * [kotlinx.coroutines.flow.debounce]와 유사하지만, [ThrottleBehavior]에 따라서 전, 후, 모두 값을 방출할 수 있다.
 *
 * * Example [ThrottleBehavior.LEADING]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime { 500.milliseconds }
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 1, 4, 7, 10
 * ```
 *
 * * Example [ThrottleBehavior.TRAILING]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(ThrottleBehavior.TRAILING) { 500.milliseconds }
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 3, 6, 9, 10
 * ```
 *
 * * Example [ThrottleBehavior.BOTH]:
 *
 * ```kotlin
 * (1..10)
 *     .asFlow()
 *     .onEach { delay(200) }
 *     .throttleTime(ThrottleBehavior.BOTH) { 500.milliseconds }
 * ```
 *
 * produces the following emissions
 *
 * ```text
 * 1, 3, 4, 6, 7, 9, 10
 * ```
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
