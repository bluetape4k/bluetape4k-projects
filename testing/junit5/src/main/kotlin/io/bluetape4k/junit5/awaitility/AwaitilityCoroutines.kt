package io.bluetape4k.junit5.awaitility

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.awaitility.core.ConditionFactory

/**
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds coAwait { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend inline infix fun ConditionFactory.coAwait(crossinline block: suspend () -> Unit) {
    coUntil { block(); true }
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
suspend inline infix fun ConditionFactory.coUntil(
    crossinline block: suspend () -> Boolean,
) = coroutineScope {
    while (isActive) {
        // print("coUntil ...")
        if (block()) {
            break
        } else {
            delay(10)    // 10ms 동안 대기
        }
    }
}
