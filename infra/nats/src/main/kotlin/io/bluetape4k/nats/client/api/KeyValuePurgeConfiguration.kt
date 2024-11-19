package io.bluetape4k.nats.client.api

import io.nats.client.api.KeyValuePurgeOptions

inline fun keyValuePurgeOptions(
    initializer: KeyValuePurgeOptions.Builder.() -> Unit,
): KeyValuePurgeOptions =
    KeyValuePurgeOptions.builder().apply(initializer).build()
