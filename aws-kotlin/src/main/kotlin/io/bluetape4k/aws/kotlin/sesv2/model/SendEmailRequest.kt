package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.Destination
import aws.sdk.kotlin.services.sesv2.model.EmailContent
import aws.sdk.kotlin.services.sesv2.model.SendEmailRequest
import io.bluetape4k.support.requireNotBlank

/**
 * 발신자 주소, 수신자, 이메일 콘텐츠로 [SendEmailRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = sendEmailRequestOf(
 *     fromEmailAddress = "sender@example.com",
 *     destination = destinationOf("user@example.com"),
 *     content = emailContent,
 * )
 * ```
 *
 * @param fromEmailAddress 발신자 이메일 주소 (비어 있으면 안 됨)
 * @param destination 수신자 [Destination]
 * @param content 전송할 [EmailContent]
 * @return [SendEmailRequest] 인스턴스
 */
fun sendEmailRequestOf(
    fromEmailAddress: String,
    destination: Destination,
    content: EmailContent,
    @BuilderInference builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest {
    fromEmailAddress.requireNotBlank("fromEmailAddress")

    return SendEmailRequest {
        this.fromEmailAddress = fromEmailAddress
        this.destination = destination
        this.content = content

        builder()
    }
}

@Deprecated(
    message = "오탈자 함수명입니다. sendEmailRequestOf를 사용하세요.",
    replaceWith =
        ReplaceWith(
            expression = "sendEmailRequestOf(fromEmailAddress, destination, content, builder)",
            imports = ["io.bluetape4k.aws.kotlin.sesv2.model.sendEmailRequestOf"]
        )
)
fun sendMailRequestOf(
    fromEmailAddress: String,
    destination: Destination,
    content: EmailContent,
    @BuilderInference builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest = sendEmailRequestOf(fromEmailAddress, destination, content, builder)
