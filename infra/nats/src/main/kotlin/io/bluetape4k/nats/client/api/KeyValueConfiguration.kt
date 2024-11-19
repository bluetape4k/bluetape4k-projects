package io.bluetape4k.nats.client.api

import io.nats.client.api.KeyValueConfiguration

inline fun keyValueConfiguration(
    name: String,
    initializer: KeyValueConfiguration.Builder.() -> Unit = {},
): KeyValueConfiguration {
    return KeyValueConfiguration.builder(name).apply(initializer).build()
}

inline fun keyValueConfiguration(
    kvConfig: KeyValueConfiguration? = null,
    initializer: KeyValueConfiguration.Builder.() -> Unit,
): KeyValueConfiguration {
    return KeyValueConfiguration.builder(kvConfig).apply(initializer).build()
}

fun keyValueConfigurationOf(
    name: String,
    maxBucketSize: Long,
    replicas: Int,
): KeyValueConfiguration = keyValueConfiguration {
    this.name(name)
    this.maxBucketSize(maxBucketSize)
    this.replicas(replicas)
}
