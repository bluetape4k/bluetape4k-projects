package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.nats.client.api.KeyValueConfiguration

inline fun keyValueConfiguration(
    name: String,
    @BuilderInference builder: KeyValueConfiguration.Builder.() -> Unit = {},
): KeyValueConfiguration {
    name.requireNotBlank("name")
    
    return KeyValueConfiguration.builder(name).apply(builder).build()
}

inline fun keyValueConfiguration(
    kvConfig: KeyValueConfiguration? = null,
    @BuilderInference builder: KeyValueConfiguration.Builder.() -> Unit,
): KeyValueConfiguration {
    return KeyValueConfiguration.builder(kvConfig).apply(builder).build()
}

fun keyValueConfigurationOf(
    name: String,
    maxBucketSize: Long,
    replicas: Int,
    @BuilderInference builder: KeyValueConfiguration.Builder.() -> Unit = {},
): KeyValueConfiguration {
    name.requireNotBlank("name")
    maxBucketSize.requirePositiveNumber("maxBucketSize")
    replicas.requirePositiveNumber("replicas")

    return keyValueConfiguration {
        this.name(name)
        this.maxBucketSize(maxBucketSize)
        this.replicas(replicas)

        builder()
    }
}
