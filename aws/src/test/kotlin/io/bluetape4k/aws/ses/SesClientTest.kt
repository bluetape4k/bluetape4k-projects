package io.bluetape4k.aws.ses

import io.bluetape4k.aws.ses.model.bodyOf
import io.bluetape4k.aws.ses.model.contentOf
import io.bluetape4k.aws.ses.model.destinationOf
import io.bluetape4k.aws.ses.model.sendEmailRequest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ses.model.SendEmailResponse

/**
 * Email 전송을 위해서는 AWS SES 에 email을 등록해야 합니다.
 *
 * https://github.com/localstack/localstack/issues/339
 *
 * ```
 * $ aws ses verify-email-identity --email-address sunghyouk.bae@gmail.com --profile localstack --endpoint-url=http://localhost:4566
 * ```
 */
class SesClientTest: AbstractSesTest() {

    companion object: KLogging()

    @Test
    fun `send email`() {
        client.verifyEmailAddress { it.emailAddress(senderEmail) }
        client.verifyEmailAddress { it.emailAddress(receiverEamil) }

        val request = sendEmailRequest {
            source(senderEmail)
            destination(destinationOf(receiverEamil))
            message { mb ->
                mb.subject(contentOf("제목"))
                mb.body(bodyOf("본문", "<p1>본문</p1>"))
            }
        }

        val response: SendEmailResponse = client.send(request)
        response.messageId().shouldNotBeEmpty()
        log.debug { "response=$response" }
    }
}
