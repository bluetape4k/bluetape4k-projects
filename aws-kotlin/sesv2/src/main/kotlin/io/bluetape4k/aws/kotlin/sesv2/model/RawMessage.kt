package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.RawMessage

fun rawMessageOf(
    data: ByteArray,
): RawMessage {
    return RawMessage {
        this.data = data
    }
}
