package io.bluetape4k.aws.kotlin.ses.model

import aws.sdk.kotlin.services.ses.model.Destination
import aws.sdk.kotlin.services.ses.model.Message
import aws.sdk.kotlin.services.ses.model.SendEmailRequest
import io.bluetape4k.support.requireNotBlank

/**
 * 발신자 주소, 수신자, 메시지로 [SendEmailRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = sendEmailRequestOf(
 *     source = "sender@example.com",
 *     destination = destinationOf("user@example.com"),
 *     message = messageOf(contentOf("Hello"), textBodyOf(contentOf("Hello, World!"))),
 * )
 * ```
 *
 * @param source 발신자 이메일 주소 (비어 있으면 안 됨)
 * @param destination 수신자 [Destination]
 * @param message 전송할 [Message]
 * @return [SendEmailRequest] 인스턴스
 */
inline fun sendEmailRequestOf(
    source: String,
    destination: Destination,
    message: Message,
    crossinline builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest {
    source.requireNotBlank("source")

    return SendEmailRequest {
        this.source = source
        this.destination = destination
        this.message = message

        builder()
    }
}

@Deprecated(
    message = "오탈자 함수명입니다. sendEmailRequestOf를 사용하세요.",
    replaceWith =
        ReplaceWith(
            expression = "sendEmailRequestOf(source, destination, message, builder)",
            imports = ["io.bluetape4k.aws.kotlin.ses.model.sendEmailRequestOf"]
        )
)
inline fun sendMailRequestOf(
    source: String,
    destination: Destination,
    message: Message,
    crossinline builder: SendEmailRequest.Builder.() -> Unit = {},
): SendEmailRequest = sendEmailRequestOf(source, destination, message, builder)
