package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.nats.client.api.KeyValueConfiguration

/**
 * 버킷 이름을 지정해 [KeyValueConfiguration]을 생성합니다.
 */
inline fun keyValueConfiguration(
    name: String,
    builder: KeyValueConfiguration.Builder.() -> Unit = {},
): KeyValueConfiguration {
    name.requireNotBlank("name")
    
    return KeyValueConfiguration.builder(name).apply(builder).build()
}

/**
 * 기존 [KeyValueConfiguration]을 기반으로 복사/수정합니다.
 */
inline fun keyValueConfiguration(
    kvConfig: KeyValueConfiguration? = null,
    builder: KeyValueConfiguration.Builder.() -> Unit,
): KeyValueConfiguration {
    return KeyValueConfiguration.builder(kvConfig).apply(builder).build()
}

/**
 * 최대 버킷 크기와 복제 개수를 포함한 기본 [KeyValueConfiguration]을 생성합니다.
 */
fun keyValueConfigurationOf(
    name: String,
    maxBucketSize: Long,
    replicas: Int,
    builder: KeyValueConfiguration.Builder.() -> Unit = {},
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
