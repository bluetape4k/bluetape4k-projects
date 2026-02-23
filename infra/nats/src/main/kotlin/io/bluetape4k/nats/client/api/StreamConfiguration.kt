package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.StreamConfiguration

inline fun streamConfiguration(
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder().apply(builder).build()
}

inline fun streamConfiguration(
    streamName: String,
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    streamName.requireNotBlank("streamName")
    return streamConfiguration {
        name(streamName)
        builder()
    }
}

inline fun streamConfiguration(
    sc: StreamConfiguration,
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder(sc).apply(builder).build()
}
