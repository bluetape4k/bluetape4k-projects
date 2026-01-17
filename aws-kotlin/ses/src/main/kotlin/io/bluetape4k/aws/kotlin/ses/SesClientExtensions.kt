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
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.defaultCrtHttpEngineOf
import io.bluetape4k.utils.ShutdownQueue


/**
 * [SesClient] 인스턴스를 생성합니다.
 *
 * ```
 * val sesClient = sesClientOf(
 *  endpoint = "http://localhost:4566",
 *  region = "us-east-1",
 *  credentialsProvider = credentialsProvider
 * )
 * ````
 *
 * @param endpoint SNS endpoint URL
 * @param region AWS region
 * @param credentialsProvider AWS credentials provider
 * @param httpClientEngine [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param configurer SNS client 설정 빌더
 * @return [SesClient] 인스턴스
 */
inline fun sesClientOf(
    endpoint: String? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClientEngine: HttpClientEngine = defaultCrtHttpEngineOf(),
    crossinline configurer: SesClient.Config.Builder.() -> Unit = {},
): SesClient = SesClient {
    endpoint?.let { this.endpointUrl = Url.parse(it) }
    region?.let { this.region = it }
    credentialsProvider?.let { this.credentialsProvider = it }
    httpClient = httpClientEngine

    configurer()
}.apply {
    ShutdownQueue.register(this)
}

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
suspend fun SesClient.send(emailRequest: SendEmailRequest): SendEmailResponse =
    sendEmail(emailRequest)


/**
 * [rawEmailRequest]를 바탕으로 raw email 을 전송합니다.
 *
 * ```
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
suspend fun SesClient.sendRaw(rawEmailRequest: SendRawEmailRequest): SendRawEmailResponse =
    sendRawEmail(rawEmailRequest)

/**
 * [emailRequest]를 바탕으로 템플릿을 사용한 email 을 전송합니다.
 *
 * ```
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
suspend fun SesClient.sendTemplated(emailRequest: SendTemplatedEmailRequest): SendTemplatedEmailResponse =
    sendTemplatedEmail(emailRequest)

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
suspend fun SesClient.sendBulkTemplated(emailRequest: SendBulkTemplatedEmailRequest): SendBulkTemplatedEmailResponse =
    sendBulkTemplatedEmail(emailRequest)


/**
 * 새로운 [Template]을 생성합니다.
 *
 * ```
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
suspend fun SesClient.createTemplate(template: Template): CreateTemplateResponse =
    createTemplate { this.template = template }

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
suspend fun SesClient.getTemplateOrNull(templateName: String): Template? =
    getTemplate { this.templateName = templateName }.template
