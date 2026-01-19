package io.bluetape4k.nats.client.api

import io.nats.client.api.ObjectStoreConfiguration

inline fun objectStoreConfiguration(
    storeName: String,
    @BuilderInference builder: ObjectStoreConfiguration.Builder.() -> Unit,
): ObjectStoreConfiguration {
    return ObjectStoreConfiguration.builder(storeName).apply(builder).build()
}

inline fun objectStoreConfiguration(
    osc: ObjectStoreConfiguration? = null,
    @BuilderInference builder: ObjectStoreConfiguration.Builder.() -> Unit,
): ObjectStoreConfiguration {
    return ObjectStoreConfiguration.builder(osc).apply(builder).build()
}
