package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Destination
import aws.sdk.kotlin.services.ses.model.Message
import aws.sdk.kotlin.services.ses.model.SendEmailRequest

inline fun sendMailRequestOf(
    source: String,
    destination: Destination,
    message: Message,
    crossinline configurer: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest = SendEmailRequest {
    this.source = source
    this.destination = destination
    this.message = message
    configurer()
}
