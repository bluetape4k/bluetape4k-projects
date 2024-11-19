package io.bluetape4k.nats.client.api

import io.nats.client.api.ObjectMetaOptions

inline fun objectMetaOptions(
    initializer: ObjectMetaOptions.Builder.() -> Unit,
): ObjectMetaOptions {
    return ObjectMetaOptions.Builder().apply(initializer).build()
}

inline fun objectMetaOptions(
    om: ObjectMetaOptions,
    initializer: ObjectMetaOptions.Builder.() -> Unit,
): ObjectMetaOptions {
    return ObjectMetaOptions.Builder(om).apply(initializer).build()
}
