package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.RawMessage

fun rawMessageOf(
    data: ByteArray,
): RawMessage {
    return RawMessage {
        this.data = data
    }
}
