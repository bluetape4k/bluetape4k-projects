package io.bluetape4k.nats.client.api

import io.nats.client.api.StreamConfiguration

inline fun streamConfiguration(
    initializer: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder().apply(initializer).build()
}

inline fun streamConfiguration(
    streamName: String,
    initializer: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration = streamConfiguration {
    name(streamName)
    initializer()
}

inline fun streamConfiguration(
    sc: StreamConfiguration,
    initializer: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder(sc).apply(initializer).build()
}
