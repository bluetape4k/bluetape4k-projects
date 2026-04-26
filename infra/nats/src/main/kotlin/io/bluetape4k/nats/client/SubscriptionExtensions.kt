package io.bluetape4k.nats.client

import io.bluetape4k.support.requireGe
import io.nats.client.Message
import io.nats.client.Subscription
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Kotlin [Duration] 기반으로 다음 메시지를 대기합니다.
 *
 * ## 동작/계약
 * - timeout은 0 이상이어야 합니다.
 * - 제한 시간 안에 메시지가 없으면 `null`을 반환합니다.
 */
fun Subscription.nextMessage(timeout: kotlin.time.Duration): Message? {
    timeout.requireGe(Duration.ZERO, "timeout")

    return nextMessage(timeout.toJavaDuration())
}
