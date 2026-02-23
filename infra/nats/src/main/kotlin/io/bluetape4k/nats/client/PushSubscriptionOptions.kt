package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.PushSubscribeOptions

inline fun pushSubscriptionOptions(
    @BuilderInference builder: PushSubscribeOptions.Builder.() -> Unit,
): PushSubscribeOptions =
    PushSubscribeOptions.builder().apply(builder).build()


fun pushSubscriptionOf(stream: String): PushSubscribeOptions {
    stream.requireNotBlank("stream")

    return PushSubscribeOptions.stream(stream)
}

fun pushSubscriptionOf(stream: String, durable: String): PushSubscribeOptions {
    stream.requireNotBlank("stream")
    durable.requireNotBlank("durable")

    return PushSubscribeOptions.bind(stream, durable)
}
