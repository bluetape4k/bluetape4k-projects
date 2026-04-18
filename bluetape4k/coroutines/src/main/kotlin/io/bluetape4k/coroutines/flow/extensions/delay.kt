package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 지정한 지연 후 `0L` 1건을 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - 지연 시간 후 단일 값 `0L`을 방출하고 완료됩니다.
 * - 수신 객체를 변경하지 않고 새 cold Flow를 반환합니다.
 * - 음수 duration은 내부적으로 `0ms`로 보정됩니다.
 *
 * ```kotlin
 * val out = flowOfDelay(500L).toList()
 * // out == [0L]
 * ```
 * @param initialDelay 초기 지연 시간입니다.
 */
fun flowOfDelay(initialDelay: Duration): Flow<Long> = delayedFlow(initialDelay)

/**
 * 지정한 밀리초 지연 후 `0L` 1건을 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - 동작은 [flowOfDelay]와 동일하며 시간 단위만 밀리초입니다.
 * - 음수 값은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOfDelay(1000L).toList()
 * // out == [0L]
 * ```
 * @param initialDelayMillis 초기 지연 시간(밀리초)입니다.
 */
fun flowOfDelay(initialDelayMillis: Long): Flow<Long> = delayedFlow(initialDelayMillis)

/**
 * [flowOfDelay]의 별칭입니다.
 *
 * ## 동작/계약
 * - 지정한 지연 후 `0L`을 1건 방출하고 완료됩니다.
 * - 음수 duration은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowWithDelay(Duration.ofSeconds(5L)).toList()
 * // out == [0L]
 * ```
 * @param initialDelay 초기 지연 시간입니다.
 */
fun flowWithDelay(initialDelay: Duration): Flow<Long> = delayedFlow(initialDelay)

/**
 * [flowOfDelay]의 밀리초 별칭입니다.
 *
 * ## 동작/계약
 * - 지정한 지연 후 `0L`을 1건 방출하고 완료됩니다.
 * - 음수 값은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowWithDelay(3000L).toList()
 * // out == [0L]
 * ```
 * @param initialDelayMillis 초기 지연 시간(밀리초)입니다.
 */
fun flowWithDelay(initialDelayMillis: Long): Flow<Long> = delayedFlow(initialDelayMillis)

/**
 * 지정한 지연 후 `0L`을 방출하는 기본 구현입니다.
 *
 * ## 동작/계약
 * - [Duration]을 밀리초로 변환해 [delayedFlow](`Long`)에 위임합니다.
 * - 음수 duration은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = delayedFlow(3.seconds).toList()
 * // out == [0L]
 * ```
 * @param delay 지연 시간입니다.
 */
fun delayedFlow(delay: Duration): Flow<Long> = delayedFlow(delay.inWholeMilliseconds)

/**
 * 지정한 밀리초 지연 후 `0L`을 1건 방출합니다.
 *
 * ## 동작/계약
 * - 수집 시점에 `delay(max(delayMillis, 0))`를 수행한 뒤 `0L`을 방출합니다.
 * - 음수 값은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = delayedFlow(1000L).toList()
 * // out == [0L]
 * ```
 * @param delayMillis 지연 시간(밀리초)입니다.
 */
fun delayedFlow(delayMillis: Long): Flow<Long> = flow {
    delay(delayMillis.coerceAtLeast(0L).milliseconds)
    emit(0L)
}

/**
 * 지정한 값을 지연 후 1건 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - [initialDelay] 후 [value]를 1건 방출하고 완료됩니다.
 * - 음수 duration은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowWithDelay(1, 3.seconds).toList()
 * // out == [1]
 * ```
 * @param value 지연 후 방출할 값입니다.
 * @param initialDelay 초기 지연 시간입니다.
 */
fun <T> flowWithDelay(value: T, initialDelay: Duration): Flow<T> = delayedFlow(value, initialDelay)

/**
 * 지정한 값을 밀리초 지연 후 1건 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - 동작은 [flowWithDelay]와 동일하며 시간 단위만 밀리초입니다.
 * - 음수 값은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowWithDelay(1, 3000L).toList()
 * // out == [1]
 * ```
 * @param value 지연 후 방출할 값입니다.
 * @param initialDelayMillis 초기 지연 시간(밀리초)입니다.
 */
fun <T> flowWithDelay(value: T, initialDelayMillis: Long): Flow<T> = delayedFlow(value, initialDelayMillis)

/**
 * 지정한 값을 [Duration] 지연 후 1건 방출합니다.
 *
 * ## 동작/계약
 * - [Duration]을 밀리초로 변환해 [delayedFlow](`value`, `Long`)에 위임합니다.
 * - 음수 duration은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = delayedFlow(1, 3.seconds).toList()
 * // out == [1]
 * ```
 * @param value 지연 후 방출할 값입니다.
 * @param duration 지연 시간입니다.
 */
fun <T> delayedFlow(value: T, duration: Duration): Flow<T> = delayedFlow(value, duration.inWholeMilliseconds)

/**
 * 지정한 값을 밀리초 지연 후 1건 방출합니다.
 *
 * ## 동작/계약
 * - 수집 시점에 `delay(max(delayMillis, 0))` 후 [value]를 1건 방출하고 완료됩니다.
 * - 음수 값은 `0ms`로 보정됩니다.
 * - 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = delayedFlow(1, 3000L).toList()
 * // out == [1]
 * ```
 * @param value 지연 후 방출할 값입니다.
 * @param delayMillis 지연 시간(밀리초)입니다.
 */
fun <T> delayedFlow(value: T, delayMillis: Long): Flow<T> = flow {
    delay(delayMillis.coerceAtLeast(0).milliseconds)
    emit(value)
}
