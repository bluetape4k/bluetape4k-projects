package io.bluetape4k.aws.kotlin.sesv2

import aws.sdk.kotlin.services.sesv2.SesV2Client
import aws.sdk.kotlin.services.sesv2.model.GetEmailTemplateRequest
import aws.sdk.kotlin.services.sesv2.model.SendBulkEmailRequest
import aws.sdk.kotlin.services.sesv2.model.SendBulkEmailResponse
import aws.sdk.kotlin.services.sesv2.model.SendEmailRequest
import aws.sdk.kotlin.services.sesv2.model.SendEmailResponse
import aws.sdk.kotlin.services.sesv2.model.Template

/**
 * [emailRequest]를 바탕으로 email 을 전송합니다.
 *
 * ```
 * val request = SendEmailRequest {
 *      destination {
 *         toAddresses = listOf("user1@example.com", "user2@example.com")
 *      }
 *      message {
 *          subject {
 *             data = "Hello, world!"
 *          }
 *          body {
 *             text {
 *                 data = "Hello, world!"
 *             }
 *             html {
 *                 data = "<h1>Hello, world!</h1>"
 *             }
 *          }
 *     }
 *     source = "noreply@example.com"
 *  }
 * // 메일 발송
 * val response = sesClient.send(request)
 * ```
 * @param emailRequest [SendEmailRequest] email 전송 요청 정보
 * @return [SendEmailResponse] email 전송 결과
 */
suspend fun SesV2Client.send(emailRequest: SendEmailRequest): SendEmailResponse =
    sendEmail(emailRequest)

/**
 * [emailRequest]를 바탕으로 템플릿을 사용한 email 을 벌크로 전송합니다.
 *
 * ```
 * val request = SendBulkTemplatedEmailRequest {
 *      defaultTemplate {
 *          templateName = "default-template"
 *          templateData = """{"name": "John Doe"}"""
 *          subject = "Hello, world!"
 *          html = "<h1>Hello, world!</h1>"
 *          text = "Hello, world!"
 *          replyToAddresses = listOf("no-reply@example.com")
 *    }
 *    source = "sender@example.com"
 * }
 *
 * val response = sesClient.sendBulkTemplated(request)
 * ```
 *
 * @param emailRequest [SendBulkTemplatedEmailRequest] email 전송 요청 정보
 * @return [SendBulkTemplatedEmailResponse] email 전송 결과
 */
suspend fun SesV2Client.sendBulk(emailRequest: SendBulkEmailRequest): SendBulkEmailResponse =
    sendBulkEmail(emailRequest)


/**
 * 등록된 [Template] 를 [templateName]으로 찾아서 반환합니다.
 *
 * ```
 * val template = sesClient.getTemplate("template-name")
 * ```
 *
 * @param templateName 템플릿 이름
 * @return [Template] 템플릿 정보, 없으면 null
 */
suspend fun SesV2Client.getTemplateOrNull(templateName: String): Template? {
    return runCatching {
        val response = getEmailTemplate(GetEmailTemplateRequest { this.templateName = templateName })
        response.templateContent?.let {
            Template {
                this.templateName = response.templateName
                this.templateContent = response.templateContent
            }
        }
    }.getOrNull()
}
