package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * 초기 지연시간[initialDelay] 이후에 0을 emit 하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = flowOfDelay(2.seconds)  // 2초 후 0을 emit
 * ```
 *
 * @param initialDelay 첫번째 요소를 보내기 전의 초기 지연 시간
 */
fun flowOfDelay(initialDelay: Duration): Flow<Long> = delayedFlow(initialDelay)

/**
 * 초기 지연시간[initialDelayMillis] 이후에 0을 emit 하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = flowOfDelay(2.seconds)  // 2초 후 0을 emit
 * ```
 *
 * @param initialDelayMillis 첫번째 요소를 보내기 전의 초기 지연 시간 (밀리초)
 */
fun flowOfDelay(initialDelayMillis: Long): Flow<Long> = delayedFlow(initialDelayMillis)

/**
 * 초기 지연시간[initialDelay] 이후에 0을 emit 하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = flowWithDelay(2.seconds)  // 2초 후 0을 emit
 * ```
 *
 * @param initialDelay 첫번째 요소를 보내기 전의 초기 지연 시간
 */
fun flowWithDelay(initialDelay: Duration): Flow<Long> = delayedFlow(initialDelay)

/**
 * 초기 지연시간[initialDelayMillis] 이후에 0을 emit 하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = flowWithDelay(2.seconds)  // 2초 후 0을 emit
 * ```
 *
 * @param initialDelayMillis 첫번째 요소를 보내기 전의 초기 지연 시간 (밀리초)
 */
fun flowWithDelay(initialDelayMillis: Long): Flow<Long> = delayedFlow(initialDelayMillis)

/**
 * 지연 시간[delay] 이후에 0을 발행하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = delayedFlow(2.seconds)  // 2초 후 0을 emit
 * ```
 *
 * @param delay 지연 시간
 * @return 지연 시간 이후 0을 발행하는 [Flow]
 */
fun delayedFlow(delay: Duration): Flow<Long> = delayedFlow(delay.inWholeMilliseconds)

/**
 * 지연 시간[delayMillis] 이후에 0을 발행하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = delayedFlow(2_000L)  // 2초 후 0을 emit
 * ```
 *
 * @param delayMillis 지연 시간 (밀리초)
 * @return 지연 시간 이후 0을 발행하는 [Flow]
 */
fun delayedFlow(delayMillis: Long): Flow<Long> = flow {
    delay(delayMillis.coerceAtLeast(0L))
    emit(0L)
}

/**
 * 지연 시간[initialDelay] 후 [value] 를 emit 하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = flowWithDelay(1, 2.seconds)  // 2초 후 1을 emit
 * ```
 *
 * @param value emit 할 값
 * @param initialDelay 초기 지연 시간
 */
fun <T> flowWithDelay(value: T, initialDelay: Duration): Flow<T> = delayedFlow(value, initialDelay)

/**
 * 지연 시간[initialDelayMillis] 후 [value] 를 emit 하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = flowWithDelay(1, 2_000L)  // 2초 후 1을 emit
 * ```
 *
 * @param value emit 할 값
 * @param initialDelayMillis 초기 지연 시간 (밀리초)
 */
fun <T> flowWithDelay(value: T, initialDelayMillis: Long): Flow<T> = delayedFlow(value, initialDelayMillis)

/**
 * 지연 시간[delay] 후 [value]를 발행하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = delayedFlow(1, 2.seconds)
 * ```
 */
fun <T> delayedFlow(value: T, duration: Duration): Flow<T> = delayedFlow(value, duration.inWholeMilliseconds)

/**
 * 지연 시간[delay] 후 [value]를 발행하는 [Flow] 를 생성합니다.
 *
 * ```
 * val flow = delayedFlow(1, 2_000L)
 * ```
 */
fun <T> delayedFlow(value: T, delayMillis: Long): Flow<T> = flow {
    delay(delayMillis.coerceAtLeast(0))
    emit(value)
}
