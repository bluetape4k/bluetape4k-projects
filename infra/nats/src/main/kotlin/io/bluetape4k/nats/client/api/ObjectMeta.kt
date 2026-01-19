package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.ObjectMeta

inline fun objectMeta(
    objectName: String,
    @BuilderInference builder: ObjectMeta.Builder.() -> Unit,
): ObjectMeta {
    objectName.requireNotBlank("objectName")
    return ObjectMeta.builder(objectName).apply(builder).build()
}

inline fun objectMeta(
    om: ObjectMeta,
    @BuilderInference builder: ObjectMeta.Builder.() -> Unit,
): ObjectMeta {
    return ObjectMeta.builder(om).apply(builder).build()
}
