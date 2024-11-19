package io.bluetape4k.nats.client.api

import io.nats.client.api.ObjectStoreConfiguration

inline fun objectStoreConfiguration(
    storeName: String,
    initializer: ObjectStoreConfiguration.Builder.() -> Unit,
): ObjectStoreConfiguration {
    return ObjectStoreConfiguration.builder(storeName).apply(initializer).build()
}

inline fun objectStoreConfiguration(
    osc: ObjectStoreConfiguration? = null,
    initializer: ObjectStoreConfiguration.Builder.() -> Unit,
): ObjectStoreConfiguration {
    return ObjectStoreConfiguration.builder(osc).apply(initializer).build()
}
