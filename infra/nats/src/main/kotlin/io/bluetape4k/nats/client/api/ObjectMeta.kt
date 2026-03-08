package io.bluetape4k.nats.client.api

import io.bluetape4k.support.requireNotBlank
import io.nats.client.api.ObjectMeta

/**
 * 객체 이름을 지정해 [ObjectMeta]를 생성합니다.
 */
inline fun objectMeta(
    objectName: String,
    @BuilderInference builder: ObjectMeta.Builder.() -> Unit,
): ObjectMeta {
    objectName.requireNotBlank("objectName")
    return ObjectMeta.builder(objectName).apply(builder).build()
}

/**
 * 기존 [ObjectMeta]를 기반으로 복사/수정합니다.
 */
inline fun objectMeta(
    om: ObjectMeta,
    @BuilderInference builder: ObjectMeta.Builder.() -> Unit,
): ObjectMeta {
    return ObjectMeta.builder(om).apply(builder).build()
}
