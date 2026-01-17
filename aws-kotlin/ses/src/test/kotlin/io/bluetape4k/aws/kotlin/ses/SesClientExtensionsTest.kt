package io.bluetape4k.aws.kotlin.ses

import aws.sdk.kotlin.services.ses.model.SendEmailRequest
import aws.sdk.kotlin.services.ses.model.SendRawEmailRequest
import aws.sdk.kotlin.services.ses.model.SendTemplatedEmailRequest
import aws.sdk.kotlin.services.ses.model.Template
import aws.sdk.kotlin.services.ses.model.VerifyEmailAddressRequest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Email 전송을 위해서는 AWS SES 에 email을 등록해야 합니다.
 *
 * https://github.com/localstack/localstack/issues/339
 *
 * ```
 * $ aws ses verify-email-identity --email-address sunghyouk.bae@gmail.com --profile localstack --endpoint-url=http://localhost:4566
 * ```
 */
class SesClientExtensionsTest: AbstractKotlinSesTest() {

    companion object: KLoggingChannel()

    @BeforeEach
    fun setup() {
        runSuspendIO {
            // 테스트 시 사용할 email 주소를 등록합니다.
            sesClient.verifyEmailAddress(VerifyEmailAddressRequest.invoke { this.emailAddress = senderEmail })
            sesClient.verifyEmailAddress(VerifyEmailAddressRequest.invoke { this.emailAddress = receiverEmail })
        }
    }

    @Test
    fun `send email`() = runSuspendIO {
        val request = SendEmailRequest {
            source = senderEmail
            destination {
                toAddresses = listOf(receiverEmail)
            }
            message {
                subject { data = "제목" }
                body {
                    text { data = "본문" }
                    html { data = "<p1>본문</p1>" }
                }
            }
        }
        val response = sesClient.sendEmail(request)
        log.debug { "response=$response" }
        response.messageId.shouldNotBeEmpty()
    }

    @Test
    fun `send raw email`() = runSuspendIO {
        val request = SendRawEmailRequest {
            source = senderEmail
            destinations = listOf(receiverEmail)
            rawMessage {
                data = "Hello, world!".toUtf8Bytes()
            }
        }
        val response = sesClient.sendRawEmail(request)
        log.debug { "response=$response" }
        response.messageId.shouldNotBeEmpty()
    }

    @Test
    fun `send templated email`() = runSuspendIO {
        val newTemplateName = "template-name"

        // 템플릿 부터 생성해야 한다.
        val template = Template {
            templateName = newTemplateName
            subjectPart = "Hello, {{name}}"
            htmlPart = "<h1>Hello, {{name}}</h1>"
            textPart = "Hello, {{name}}"
        }

        val createTemplateResponse = sesClient.createTemplate(template)
        log.debug { "createTemplateResponse=$createTemplateResponse" }


        val savedTemplate = sesClient.getTemplateOrNull(newTemplateName)
        log.debug { "saved template=$savedTemplate" }
        savedTemplate?.templateName shouldBeEqualTo newTemplateName

        val request = SendTemplatedEmailRequest {
            source = senderEmail
            destination {
                toAddresses = listOf(receiverEmail)
            }
            this.template = newTemplateName
            templateData = """{"name": "world"}"""
        }

        val response = sesClient.sendTemplatedEmail(request)
        log.debug { "response=$response" }
        response.messageId.shouldNotBeEmpty()
    }
}
