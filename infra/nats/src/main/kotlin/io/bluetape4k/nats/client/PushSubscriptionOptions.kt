package io.bluetape4k.nats.client

import io.nats.client.PushSubscribeOptions

inline fun pushSubscriptionOptions(
    initializer: PushSubscribeOptions.Builder.() -> Unit,
): PushSubscribeOptions =
    PushSubscribeOptions.builder().apply(initializer).build()


fun pushSubscriptionOf(stream: String): PushSubscribeOptions =
    PushSubscribeOptions.stream(stream)

fun pushSubscriptionOf(stream: String, durable: String): PushSubscribeOptions =
    PushSubscribeOptions.bind(stream, durable)
