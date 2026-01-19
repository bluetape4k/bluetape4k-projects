package io.bluetape4k.nats.client

import io.nats.client.PushSubscribeOptions

inline fun pushSubscriptionOptions(
    @BuilderInference builder: PushSubscribeOptions.Builder.() -> Unit,
): PushSubscribeOptions =
    PushSubscribeOptions.builder().apply(builder).build()


fun pushSubscriptionOf(stream: String): PushSubscribeOptions =
    PushSubscribeOptions.stream(stream)

fun pushSubscriptionOf(stream: String, durable: String): PushSubscribeOptions =
    PushSubscribeOptions.bind(stream, durable)
