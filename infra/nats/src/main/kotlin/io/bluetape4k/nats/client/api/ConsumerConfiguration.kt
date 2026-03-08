package io.bluetape4k.nats.client.api

import io.nats.client.api.ConsumerConfiguration

/**
 * 기존 설정을 기반으로 하거나 새 [ConsumerConfiguration]을 DSL로 생성합니다.
 */
inline fun consumerConfiguration(
    cc: ConsumerConfiguration? = null,
    @BuilderInference builder: ConsumerConfiguration.Builder.() -> Unit,
): ConsumerConfiguration =
    ConsumerConfiguration.builder(cc).apply(builder).build()
