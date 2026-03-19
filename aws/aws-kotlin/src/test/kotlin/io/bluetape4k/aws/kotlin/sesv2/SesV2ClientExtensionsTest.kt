package io.bluetape4k.aws.kotlin.sesv2

import aws.sdk.kotlin.services.sesv2.model.CreateEmailTemplateRequest
import aws.sdk.kotlin.services.sesv2.model.SendCustomVerificationEmailRequest
import aws.sdk.kotlin.services.sesv2.model.SendEmailRequest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
@Disabled("LocalStack에서 SES V2를 지원하지 않습니다.")
class SesV2ClientExtensionsTest: AbstractKotlinSesV2Test() {

    companion object: KLoggingChannel()

    @BeforeEach
    fun setup() {
        runSuspendIO {
            // 테스트 시 사용할 email 주소를 등록합니다.
            sesV2Client.sendCustomVerificationEmail(SendCustomVerificationEmailRequest {
                this.emailAddress = senderEmail
            })
            sesV2Client.sendCustomVerificationEmail(SendCustomVerificationEmailRequest {
                this.emailAddress = receiverEmail
            })
        }
    }

    @Test
    fun `send email`() = runSuspendIO {
        val request = SendEmailRequest {
            fromEmailAddress = senderEmail
            destination {
                toAddresses = listOf(receiverEmail)
            }
            content {
                simple {
                    subject { data = "제목" }
                    body {
                        text { data = "본문" }
                        html { data = "<p1>본문</p1>" }
                    }
                }
            }
        }
        val response = sesV2Client.sendEmail(request)
        log.debug { "response=$response" }
        response.messageId.shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    fun `send raw email`() = runSuspendIO {
        val request = SendEmailRequest {
            fromEmailAddress = senderEmail
            destination {
                toAddresses = listOf(receiverEmail)
            }
            content {
                raw {
                    data = "Hello, world!".toUtf8Bytes()
                }
            }
        }
        val response = sesV2Client.sendEmail(request)
        log.debug { "response=$response" }
        response.messageId.shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    fun `send templated email`() = runSuspendIO {
        val newTemplateName = "template-name"

        // 템플릿 부터 생성해야 한다.
        val createTemplateRequest = CreateEmailTemplateRequest {
            templateName = newTemplateName

            templateContent {
                subject = "Hello, {{name}}"
                html = "<h1>Hello, {{name}}</h1>"
                text = "Hello, {{name}}"
            }
        }

        val createTemplateResponse = sesV2Client.createEmailTemplate(createTemplateRequest)
        log.debug { "createTemplateResponse=$createTemplateResponse" }

        val savedTemplate = sesV2Client.getTemplateOrNull(newTemplateName)
        log.debug { "saved template=$savedTemplate" }
        savedTemplate?.templateName shouldBeEqualTo newTemplateName

        val request = SendEmailRequest {
            fromEmailAddress = senderEmail
            destination {
                toAddresses = listOf(receiverEmail)
            }
            content {
                template {
                    templateName = newTemplateName
                    templateData = """{"name": "world"}"""
                }
            }
        }

        val response = sesV2Client.sendEmail(request)
        log.debug { "response=$response" }
        response.messageId.shouldNotBeNull().shouldNotBeEmpty()
    }
}
