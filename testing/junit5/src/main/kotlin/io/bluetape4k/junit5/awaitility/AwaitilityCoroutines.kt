package io.bluetape4k.junit5.awaitility

import kotlinx.coroutines.runBlocking
import org.awaitility.Durations
import org.awaitility.core.ConditionFactory
import java.time.Duration

private val DEFAULT_POLL_INTERVAL: Duration = Durations.ONE_HUNDRED_MILLISECONDS

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds suspendAwait { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend infix fun ConditionFactory.awaitSuspending(
    block: suspend () -> Unit,
) {
    untilSuspending { block(); true }
}

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds suspendUntil { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend infix fun ConditionFactory.untilSuspending(
    block: suspend () -> Boolean,
) {
    until {
        runBlocking {
            block()
        }
    }
}
