package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Destination
import aws.sdk.kotlin.services.sesv2.model.EmailContent
import aws.sdk.kotlin.services.sesv2.model.SendEmailRequest

fun sendEmailRequestOf(
    fromEmailAddress: String,
    destination: Destination,
    content: EmailContent,
    @BuilderInference builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest {
    require(fromEmailAddress.isNotBlank()) { "fromEmailAddress must not be blank." }

    return SendEmailRequest {
        this.fromEmailAddress = fromEmailAddress
        this.destination = destination
        this.content = content

        builder()
    }
}

@Deprecated(
    message = "오탈자 함수명입니다. sendEmailRequestOf를 사용하세요.",
    replaceWith = ReplaceWith(
        expression = "sendEmailRequestOf(fromEmailAddress, destination, content, builder)",
        imports = ["io.bluetape4k.aws.kotlin.sesv2.model.sendEmailRequestOf"],
    ),
)
fun sendMailRequestOf(
    fromEmailAddress: String,
    destination: Destination,
    content: EmailContent,
    @BuilderInference builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest =
    sendEmailRequestOf(fromEmailAddress, destination, content, builder)
