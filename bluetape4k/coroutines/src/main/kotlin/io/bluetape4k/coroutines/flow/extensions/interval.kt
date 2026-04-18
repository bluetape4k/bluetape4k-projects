package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * source 방출 앞에 초기 지연과 요소 간 지연을 적용합니다.
 *
 * ## 동작/계약
 * - `initialDelay`, `delay`는 음수면 0으로 보정합니다.
 * - 초기 지연 후 source를 수집하며 각 요소 emit 직전에 `delay`를 적용합니다.
 * - `delay == 0`이면 추가 지연 없이 원소를 그대로 전달합니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2).interval(100.milliseconds, 50.milliseconds).toList()
 * // result == [1, 2] (방출 시점만 지연)
 * ```
 *
 * @param initialDelay 첫 요소 이전 초기 지연입니다.
 * @param delay 요소 간 지연입니다.
 */
fun <T> Flow<T>.interval(
    initialDelay: Duration,
    delay: Duration = Duration.ZERO,
): Flow<T> = flow {
    val initialDelayValue = initialDelay.coerceAtLeast(Duration.ZERO)
    val delayValue = delay.coerceAtLeast(Duration.ZERO)

    if (initialDelayValue.isPositive()) {
        delay(initialDelayValue)
    }

    if (delayValue.isPositive()) {
        this@interval.collect { value ->
            delay(delayValue)
            emit(value)
        }
    } else {
        this@interval.collect { value ->
            emit(value)
        }
    }
}

/**
 * 밀리초 단위로 초기 지연과 요소 간 지연을 적용합니다.
 *
 * ## 동작/계약
 * - `initialDelayMillis`, `delayMillis`는 음수면 0으로 보정합니다.
 * - 동작은 Duration 오버로드와 동일합니다.
 *
 * @param initialDelayMillis 첫 요소 이전 초기 지연(밀리초)입니다.
 * @param delayMillis 요소 간 지연(밀리초)입니다.
 */
fun <T> Flow<T>.interval(
    initialDelayMillis: Long = 0L,
    delayMillis: Long = 0L,
): Flow<T> = flow {
    val initialDelayValue = initialDelayMillis.coerceAtLeast(0L)
    val delayValue = delayMillis.coerceAtLeast(0L)

    if (initialDelayValue > 0L) {
        delay(initialDelayValue.milliseconds)
    }

    if (delayValue > 0L) {
        this@interval.collect { value ->
            delay(delayValue.milliseconds)
            emit(value)
        }
    } else {
        this@interval.collect { value ->
            emit(value)
        }
    }
}

/**
 * 무한 증가 시퀀스를 주기적으로 방출하는 interval Flow를 생성합니다.
 *
 * ## 동작/계약
 * - `initialDelay` 이후 0부터 시작하는 `Long` 값을 1씩 증가시켜 무한 방출합니다.
 * - `delay`는 음수면 0으로 보정합니다.
 * - 취소될 때까지 종료되지 않습니다.
 *
 * ```kotlin
 * val firstThree = intervalFlowOf(Duration.ZERO, 100.milliseconds).take(3).toList()
 * // firstThree == [0, 1, 2]
 * ```
 *
 * @param initialDelay 첫 방출 전 지연입니다.
 * @param delay 방출 간 지연입니다.
 */
fun intervalFlowOf(initialDelay: Duration, delay: Duration): Flow<Long> = flow {
    val initialDelayValue = initialDelay.coerceAtLeast(Duration.ZERO)
    val delayValue = delay.coerceAtLeast(Duration.ZERO)

    delay(initialDelayValue)
    var sequencer = 0L
    while (true) {
        emit(sequencer++)
        delay(delayValue)
    }
}

/**
 * 밀리초 단위 interval Flow를 생성합니다.
 *
 * ## 동작/계약
 * - 동작은 Duration 오버로드와 동일하며 지연값만 밀리초 입력을 사용합니다.
 * - 음수 입력은 0으로 보정합니다.
 *
 * @param initialDelayMillis 첫 방출 전 지연(밀리초)입니다.
 * @param delayMillis 방출 간 지연(밀리초)입니다.
 */
fun intervalFlowOf(initialDelayMillis: Long, delayMillis: Long): Flow<Long> = flow {
    val initialDelayValue = initialDelayMillis.coerceAtLeast(0L)
    val delayValue = delayMillis.coerceAtLeast(0L)

    delay(initialDelayValue.milliseconds)
    var sequencer = 0L
    while (true) {
        emit(sequencer++)
        delay(delayValue.milliseconds)
    }
}
