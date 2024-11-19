package io.bluetape4k.nats.client

import io.nats.client.PullSubscribeOptions

inline fun pullSubscriptionOptions(
    initializer: PullSubscribeOptions.Builder.() -> Unit,
): PullSubscribeOptions =
    PullSubscribeOptions.builder().apply(initializer).build()

fun pullSubscriptionOptionsOf(stream: String, bind: String): PullSubscribeOptions =
    PullSubscribeOptions.bind(stream, bind)
