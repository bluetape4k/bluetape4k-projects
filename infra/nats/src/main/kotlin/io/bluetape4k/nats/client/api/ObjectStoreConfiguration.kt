package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.ObjectStoreConfiguration

/**
 * 버킷 이름을 지정해 [ObjectStoreConfiguration]을 생성합니다.
 */
inline fun objectStoreConfiguration(
    storeName: String,
    @BuilderInference builder: ObjectStoreConfiguration.Builder.() -> Unit,
): ObjectStoreConfiguration {
    storeName.requireNotBlank("storeName")

    return ObjectStoreConfiguration.builder(storeName).apply(builder).build()
}

/**
 * 기존 [ObjectStoreConfiguration]을 기반으로 복사/수정합니다.
 */
inline fun objectStoreConfiguration(
    osc: ObjectStoreConfiguration? = null,
    @BuilderInference builder: ObjectStoreConfiguration.Builder.() -> Unit,
): ObjectStoreConfiguration {
    return ObjectStoreConfiguration.builder(osc).apply(builder).build()
}
