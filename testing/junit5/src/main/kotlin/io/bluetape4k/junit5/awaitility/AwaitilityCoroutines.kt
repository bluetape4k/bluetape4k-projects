package io.bluetape4k.junit5.awaitility

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.delay
import org.awaitility.core.ConditionFactory
import java.time.Duration

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds coAwait { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend inline infix fun ConditionFactory.suspendAwait(crossinline block: suspend () -> Unit) {
    suspendUntil { block(); true }
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
suspend inline infix fun ConditionFactory.suspendUntil(
    crossinline block: suspend () -> Boolean,
) = coroutineScope {
    while (isActive) {
        // print("coUntil ...")
        if (block()) {
            break
        } else {
            delay(10L)    // 10ms 동안 대기
        }
    }
}

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds coAwait { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend inline fun ConditionFactory.suspendAwait(
    pollInterval: Duration = Duration.ofMillis(10),
    crossinline block: suspend () -> Unit,
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
suspend inline fun ConditionFactory.suspendUntil(
    pollInterval: Duration = Duration.ofMillis(10),
    crossinline block: suspend () -> Boolean,
) = coroutineScope {
    while (isActive) {
        // print("coUntil ...")
        if (block()) {
            break
        } else {
            delay(pollInterval)    // 10ms 동안 대기
        }
    }
}
