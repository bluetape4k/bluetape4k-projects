package io.bluetape4k.nats.client

import io.bluetape4k.support.requireGe
import io.nats.client.Message
import io.nats.client.Subscription
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun Subscription.nextMessage(timeout: kotlin.time.Duration): Message {
    timeout.requireGe(Duration.ZERO, "timeout")

    return nextMessage(timeout.toJavaDuration())
}
