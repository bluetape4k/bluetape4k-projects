package io.bluetape4k.nats.client

import io.nats.client.Options
import java.util.*

inline fun natsOptions(initializer: Options.Builder.() -> Unit): Options {
    return Options.builder().apply(initializer).build()
}

inline fun natsOptions(
    properties: Properties,
    initializer: Options.Builder.() -> Unit = {},
): Options {
    return Options.Builder(properties).apply(initializer).build()
}

fun natsOptionsOf(
    url: String = Options.DEFAULT_URL,
    maxReconnects: Int = Options.DEFAULT_MAX_RECONNECT,
    bufferSize: Int = Options.DEFAULT_BUFFER_SIZE,
): Options = natsOptions {
    server(url)
    maxReconnects(maxReconnects)
    bufferSize(bufferSize)
}
