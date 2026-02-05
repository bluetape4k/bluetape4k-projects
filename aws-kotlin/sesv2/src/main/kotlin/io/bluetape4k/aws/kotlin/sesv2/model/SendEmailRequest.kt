package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Destination
import aws.sdk.kotlin.services.sesv2.model.EmailContent
import aws.sdk.kotlin.services.sesv2.model.SendEmailRequest

fun sendMailRequestOf(
    fromEmailAddress: String,
    destination: Destination,
    content: EmailContent,
    @BuilderInference builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest =
    SendEmailRequest {
        this.fromEmailAddress = fromEmailAddress
        this.destination = destination
        this.content = content

        builder()
    }
