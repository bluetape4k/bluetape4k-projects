package io.bluetape4k.nats.client.api

import io.nats.client.api.StreamConfiguration

inline fun streamConfiguration(
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder().apply(builder).build()
}

inline fun streamConfiguration(
    streamName: String,
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration = streamConfiguration {
    name(streamName)
    builder()
}

inline fun streamConfiguration(
    sc: StreamConfiguration,
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder(sc).apply(builder).build()
}
