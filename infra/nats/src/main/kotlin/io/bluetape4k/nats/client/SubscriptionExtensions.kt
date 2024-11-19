package io.bluetape4k.nats.client

import io.nats.client.Message
import io.nats.client.Subscription
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun Subscription.nextMessage(timeout: Duration): Message =
    nextMessage(timeout.toJavaDuration())
