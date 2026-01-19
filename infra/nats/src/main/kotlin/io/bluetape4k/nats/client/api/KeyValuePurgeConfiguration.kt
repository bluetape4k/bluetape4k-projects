package io.bluetape4k.nats.client.api

import io.nats.client.api.KeyValuePurgeOptions

inline fun keyValuePurgeOptions(
    @BuilderInference builder: KeyValuePurgeOptions.Builder.() -> Unit,
): KeyValuePurgeOptions =
    KeyValuePurgeOptions.builder().apply(builder).build()
