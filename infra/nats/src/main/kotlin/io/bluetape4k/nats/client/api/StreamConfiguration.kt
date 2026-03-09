package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.StreamConfiguration

/**
 * [StreamConfiguration]을 DSL로 생성합니다.
 */
inline fun streamConfiguration(
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder().apply(builder).build()
}

/**
 * 스트림 이름을 고정한 [StreamConfiguration]을 생성합니다.
 */
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

/**
 * 기존 설정을 기반으로 [StreamConfiguration]을 복사/수정합니다.
 */
inline fun streamConfiguration(
    sc: StreamConfiguration,
    @BuilderInference builder: StreamConfiguration.Builder.() -> Unit,
): StreamConfiguration {
    return StreamConfiguration.builder(sc).apply(builder).build()
}
