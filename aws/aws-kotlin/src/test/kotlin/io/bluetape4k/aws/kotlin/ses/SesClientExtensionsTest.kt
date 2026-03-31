package io.bluetape4k.aws.kotlin.ses

import aws.sdk.kotlin.services.ses.model.SendEmailRequest
import aws.sdk.kotlin.services.ses.model.SendRawEmailRequest
import aws.sdk.kotlin.services.ses.model.SendTemplatedEmailRequest
import aws.sdk.kotlin.services.ses.model.Template
import aws.sdk.kotlin.services.ses.model.VerifyEmailAddressRequest
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
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

    @Test
    fun `send email`() = runSuspendIO {
        withSesClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.verifyEmailAddress(VerifyEmailAddressRequest { this.emailAddress = senderEmail })
            client.verifyEmailAddress(VerifyEmailAddressRequest { this.emailAddress = receiverEmail })

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
            val response = client.send(request)
            log.debug { "response=$response" }
            response.messageId.shouldNotBeEmpty()
        }
    }

    @Test
    fun `send raw email`() = runSuspendIO {
        withSesClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.verifyEmailAddress(VerifyEmailAddressRequest { this.emailAddress = senderEmail })
            client.verifyEmailAddress(VerifyEmailAddressRequest { this.emailAddress = receiverEmail })

            val request = SendRawEmailRequest {
                source = senderEmail
                destinations = listOf(receiverEmail)
                rawMessage {
                    data = "Hello, world!".toUtf8Bytes()
                }
            }
            val response = client.sendRaw(request)
            log.debug { "response=$response" }
            response.messageId.shouldNotBeEmpty()
        }
    }

    @Test
    fun `send templated email`() = runSuspendIO {
        withSesClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.verifyEmailAddress(VerifyEmailAddressRequest { this.emailAddress = senderEmail })
            client.verifyEmailAddress(VerifyEmailAddressRequest { this.emailAddress = receiverEmail })

            val newTemplateName = "template-name-" + Base58.randomString(6)

            // 템플릿 부터 생성해야 한다.
            val template = Template {
                templateName = newTemplateName
                subjectPart = "Hello, {{name}}"
                htmlPart = "<h1>Hello, {{name}}</h1>"
                textPart = "Hello, {{name}}"
            }

            val createTemplateResponse = client.createTemplate(template)
            log.debug { "createTemplateResponse=$createTemplateResponse" }

            val savedTemplate = client.getTemplateOrNull(newTemplateName)
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

            val response = client.sendTemplated(request)
            log.debug { "response=$response" }
            response.messageId.shouldNotBeEmpty()
        }
    }

    @Test
    fun `unknown template은 null을 반환한다`() = runSuspendIO {
        withSesClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val unknown = client.getTemplateOrNull("not-exists-template")
            unknown shouldBeEqualTo null
        }
    }
}
