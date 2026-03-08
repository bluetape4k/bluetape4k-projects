package io.bluetape4k.nats.client.api

import io.nats.client.api.ObjectMetaOptions

/**
 * [ObjectMetaOptions]를 DSL로 생성합니다.
 */
inline fun objectMetaOptions(
    @BuilderInference builder: ObjectMetaOptions.Builder.() -> Unit,
): ObjectMetaOptions {
    return ObjectMetaOptions.Builder().apply(builder).build()
}

/**
 * 기존 [ObjectMetaOptions]를 기반으로 복사/수정합니다.
 */
inline fun objectMetaOptions(
    om: ObjectMetaOptions,
    @BuilderInference builder: ObjectMetaOptions.Builder.() -> Unit,
): ObjectMetaOptions {
    return ObjectMetaOptions.Builder(om).apply(builder).build()
}
