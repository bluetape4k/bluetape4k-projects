package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.PullSubscribeOptions

inline fun pullSubscriptionOptions(
    @BuilderInference builder: PullSubscribeOptions.Builder.() -> Unit,
): PullSubscribeOptions =
    PullSubscribeOptions.builder()
        .apply(builder)
        .build()

fun pullSubscriptionOptionsOf(stream: String, bind: String): PullSubscribeOptions {
    stream.requireNotBlank("stream")
    bind.requireNotBlank("bind")

    return PullSubscribeOptions.bind(stream, bind)
}
