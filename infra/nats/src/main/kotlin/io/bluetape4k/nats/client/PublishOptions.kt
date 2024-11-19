package io.bluetape4k.nats.client

import io.nats.client.PublishOptions
import java.util.*

inline fun publishOptions(
    initializer: PublishOptions.Builder.() -> Unit,
): PublishOptions {
    return PublishOptions.builder().apply(initializer).build()
}

fun publishOptionsOf(
    properties: Properties,
    initializer: PublishOptions.Builder.() -> Unit = {},
): PublishOptions {
    return PublishOptions.Builder(properties).apply(initializer).build()
}
