package io.bluetape4k.nats.client.api

import io.nats.client.api.ObjectMetaOptions

inline fun objectMetaOptions(
    @BuilderInference builder: ObjectMetaOptions.Builder.() -> Unit,
): ObjectMetaOptions {
    return ObjectMetaOptions.Builder().apply(builder).build()
}

inline fun objectMetaOptions(
    om: ObjectMetaOptions,
    @BuilderInference builder: ObjectMetaOptions.Builder.() -> Unit,
): ObjectMetaOptions {
    return ObjectMetaOptions.Builder(om).apply(builder).build()
}
