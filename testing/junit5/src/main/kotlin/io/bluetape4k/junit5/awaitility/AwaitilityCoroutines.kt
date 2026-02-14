package io.bluetape4k.junit5.awaitility

import kotlinx.coroutines.delay
import org.awaitility.Durations
import org.awaitility.core.ConditionFactory
import java.time.Duration

private val DEFAULT_POLL_INTERVAL: Duration = Durations.ONE_HUNDRED_MILLISECONDS

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds coAwait { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend infix fun ConditionFactory.suspendAwait(
    block: suspend () -> Unit,
) {
    suspendUntil(DEFAULT_POLL_INTERVAL) { block(); true }
}

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds coUntil { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend infix fun ConditionFactory.suspendUntil(
    block: suspend () -> Boolean,
) = suspendUntil(DEFAULT_POLL_INTERVAL, block)

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds coAwait { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend fun ConditionFactory.suspendAwait(
    pollInterval: Duration = DEFAULT_POLL_INTERVAL,
    block: suspend () -> Unit,
) {
    suspendUntil(pollInterval) { block(); true }
}

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds coUntil { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
@Suppress("UnusedReceiverParameter")
suspend fun ConditionFactory.suspendUntil(
    pollInterval: Duration = DEFAULT_POLL_INTERVAL,
    block: suspend () -> Boolean,
) = awaitUntilLoop(pollInterval, block)

private suspend fun awaitUntilLoop(
    pollInterval: Duration,
    block: suspend () -> Boolean,
) {
    require(!pollInterval.isNegative && !pollInterval.isZero) { "pollInterval must be positive." }
    val pollMillis = pollInterval.toMillis()

    while (true) {
        if (block()) {
            return
        }
        delay(pollMillis)
    }
}
