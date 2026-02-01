package io.bluetape4k.nats.client

import io.nats.client.Options
import java.util.*

inline fun natsOptions(
    @BuilderInference builder: Options.Builder.() -> Unit,
): Options {
    return Options.builder().apply(builder).build()
}

inline fun natsOptions(
    properties: Properties,
    @BuilderInference builder: Options.Builder.() -> Unit = {},
): Options {
    return Options.Builder(properties).apply(builder).build()
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
