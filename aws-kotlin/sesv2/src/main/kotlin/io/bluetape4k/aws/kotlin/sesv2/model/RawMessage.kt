package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.RawMessage

fun rawMessageOf(
    data: ByteArray,
): RawMessage {
    require(data.isNotEmpty()) { "data must not be empty." }

    return RawMessage {
        this.data = data
    }
}
