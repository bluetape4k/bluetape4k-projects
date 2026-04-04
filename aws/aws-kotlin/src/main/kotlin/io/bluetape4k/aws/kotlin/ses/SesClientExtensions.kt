package io.bluetape4k.aws.kotlin.ses

import aws.sdk.kotlin.services.ses.SesClient
import aws.sdk.kotlin.services.ses.createTemplate
import aws.sdk.kotlin.services.ses.getTemplate
import aws.sdk.kotlin.services.ses.model.CreateTemplateResponse
import aws.sdk.kotlin.services.ses.model.SendBulkTemplatedEmailRequest
import aws.sdk.kotlin.services.ses.model.SendBulkTemplatedEmailResponse
import aws.sdk.kotlin.services.ses.model.SendEmailRequest
import aws.sdk.kotlin.services.ses.model.SendEmailResponse
import aws.sdk.kotlin.services.ses.model.SendRawEmailRequest
import aws.sdk.kotlin.services.ses.model.SendRawEmailResponse
import aws.sdk.kotlin.services.ses.model.SendTemplatedEmailRequest
import aws.sdk.kotlin.services.ses.model.SendTemplatedEmailResponse
import aws.sdk.kotlin.services.ses.model.Template

/**
 * [emailRequest]를 바탕으로 email 을 전송합니다.
 *
 * ```kotlin
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
suspend inline fun SesClient.send(emailRequest: SendEmailRequest): SendEmailResponse =
    sendEmail(emailRequest)


/**
 * [rawEmailRequest]를 바탕으로 raw email 을 전송합니다.
 *
 * ```kotlin
 * val request = SendRawEmailRequest {
 *     rawMessage {
 *         data = "From: noreply@example.com\nTo: user1@example.com\nSubject: Hello, world!\n\nHello, world!"
 *     }
 * }
 *
 * val response = sesClient.sendRaw(request)
 * ```
 * @param rawEmailRequest [SendRawEmailRequest] raw email 전송 요청 정보
 * @return [SendRawEmailResponse] raw email 전송 결과
 */
suspend inline fun SesClient.sendRaw(rawEmailRequest: SendRawEmailRequest): SendRawEmailResponse =
    sendRawEmail(rawEmailRequest)

/**
 * [emailRequest]를 바탕으로 템플릿을 사용한 email 을 전송합니다.
 *
 * ```kotlin
 * val request = SendTemplatedEmailRequest {
 *    destination {
 *       toAddresses = listOf("user1@example.com", "user2@example.com")
 *    }
 *    template {
 *          templateName = "template-name"
 *          templateData = """{"name": "John Doe"}"""
 *          subject = "Hello, world!"
 *          html = "<h1>Hello, world!</h1>"
 *          text = "Hello, world!"
 *          replyToAddresses = listOf("no-reply@example.com")
 *    }
 *    source = "sender@example.com"
 * }
 *
 * val response = sesClient.sendTemplated(request)
 * ```
 *
 * @param emailRequest [SendTemplatedEmailRequest] email 전송 요청 정보
 * @return [SendTemplatedEmailResponse] email 전송 결과
 */
suspend inline fun SesClient.sendTemplated(emailRequest: SendTemplatedEmailRequest): SendTemplatedEmailResponse =
    sendTemplatedEmail(emailRequest)

/**
 * [emailRequest]를 바탕으로 템플릿을 사용한 email 을 벌크로 전송합니다.
 *
 * ```kotlin
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
suspend inline fun SesClient.sendBulkTemplated(emailRequest: SendBulkTemplatedEmailRequest): SendBulkTemplatedEmailResponse =
    sendBulkTemplatedEmail(emailRequest)


/**
 * 새로운 [Template]을 생성합니다.
 *
 * ```kotlin
 * val template = Template {
 *      templateName = "template-name"
 *      subjectPart = "Hello, {{name}}"
 *      htmlPart = "<h1>Hello, {{name}}</h1>"
 *      textPart = "Hello, {{name}}"
 *      replyToAddresses = listOf("no-reply@example.com")
 * }
 * val response = sesClient.createTemplate(template)
 * ```
 *
 * @param template [Template] 템플릿 정보
 */
suspend inline fun SesClient.createTemplate(template: Template): CreateTemplateResponse =
    createTemplate { this.template = template }

/**
 * 등록된 [Template] 를 [templateName]으로 찾아서 반환합니다.
 *
 * ```kotlin
 * val template = sesClient.getTemplate("template-name")
 * ```
 *
 * @param templateName 템플릿 이름
 * @return [Template] 템플릿 정보, 없으면 null
 */
suspend inline fun SesClient.getTemplateOrNull(templateName: String): Template? =
    runCatching {
        getTemplate { this.templateName = templateName }.template
    }.getOrNull()
