package io.bluetape4k.nats.client

import io.nats.client.PublishOptions
import java.util.*

inline fun publishOptions(
    @BuilderInference builder: PublishOptions.Builder.() -> Unit,
): PublishOptions =
    PublishOptions.builder()
        .apply(builder)
        .build()

inline fun publishOptionsOf(
    properties: Properties,
    @BuilderInference builder: PublishOptions.Builder.() -> Unit = {},
): PublishOptions =
    PublishOptions.Builder(properties)
        .apply(builder)
        .build()
