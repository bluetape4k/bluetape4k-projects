package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.RawMessage
import aws.sdk.kotlin.services.ses.model.SendRawEmailRequest

inline fun sendRawEmailRequestOf(
    rawMessage: RawMessage,
    crossinline configurer: SendRawEmailRequest.Builder.() -> Unit = {},
): SendRawEmailRequest = SendRawEmailRequest {
    this.rawMessage = rawMessage
    configurer()
}
