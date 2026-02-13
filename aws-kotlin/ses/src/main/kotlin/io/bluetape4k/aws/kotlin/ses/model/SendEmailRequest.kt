package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Destination
import aws.sdk.kotlin.services.ses.model.Message
import aws.sdk.kotlin.services.ses.model.SendEmailRequest

inline fun sendEmailRequestOf(
    source: String,
    destination: Destination,
    message: Message,
    @BuilderInference crossinline builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest {
    require(source.isNotBlank()) { "source must not be blank." }

    return SendEmailRequest {
        this.source = source
        this.destination = destination
        this.message = message

        builder()
    }
}

@Deprecated(
    message = "오탈자 함수명입니다. sendEmailRequestOf를 사용하세요.",
    replaceWith = ReplaceWith(
        expression = "sendEmailRequestOf(source, destination, message, builder)",
        imports = ["io.bluetape4k.aws.kotlin.ses.model.sendEmailRequestOf"],
    ),
)
inline fun sendMailRequestOf(
    source: String,
    destination: Destination,
    message: Message,
    @BuilderInference crossinline builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest =
    sendEmailRequestOf(source, destination, message, builder)
