package io.bluetape4k.nats.client.api

import io.nats.client.api.ConsumerConfiguration

inline fun consumerConfiguration(
    cc: ConsumerConfiguration? = null,
    initializer: ConsumerConfiguration.Builder.() -> Unit,
): ConsumerConfiguration =
    ConsumerConfiguration.builder(cc).apply(initializer).build()
