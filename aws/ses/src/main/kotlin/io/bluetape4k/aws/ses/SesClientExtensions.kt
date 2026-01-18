package io.bluetape4k.aws.ses

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.SesClientBuilder
import software.amazon.awssdk.services.ses.endpoints.SesEndpointProvider
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailResponse
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse

/**
 * [SesClient]를 빌드합니다.
 *
 * ```
 * val client = SesClient {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 *     region(region)
 * }
 * client.verifyEmailAddress { it.emailAddress(senderEmail) }
 * client.verifyEmailAddress { it.emailAddress(receiverEamil) }
 * client.send(request)
 * ```
 */
inline fun SesClient(builder: SesClientBuilder.() -> Unit): SesClient {
    return SesClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

/**
 * [SesClient]를 생성합니다.
 *
 * ```
 * val client = sesClientOf(region) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * client.verifyEmailAddress { it.emailAddress(senderEmail) }
 * client.verifyEmailAddress { it.emailAddress(receiverEamil) }
 * client.send(request)
 * ```
 */
inline fun sesClientOf(
    region: Region,
    builder: SesClientBuilder.() -> Unit = {},
): SesClient = SesClient {
    region(region)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    builder()
}

/**
 * [SesClient]를 생성합니다.
 *
 * ```
 * val client = sesClientOf(endpointProvider) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * client.verifyEmailAddress { it.emailAddress(senderEmail) }
 * client.verifyEmailAddress { it.emailAddress(receiverEamil) }
 * client.send(request)
 * ```
 */
inline fun sesClientOf(
    endpointProvider: SesEndpointProvider,
    builder: SesClientBuilder.() -> Unit = {},
): SesClient = SesClient {
    endpointProvider(endpointProvider)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    builder()
}

/**
 * [SendEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.send(request)
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param emailRequest [SendEmailRequest] email 전송 요청 정보
 * @return [SendEmailResponse] email 전송 응답 정보
 */
fun SesClient.send(emailRequest: SendEmailRequest): SendEmailResponse {
    return sendEmail(emailRequest)
}

/**
 * [SendRawEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.sendRaw(rawEmailRequest)
 * log.debug { "response=$response" }
 * ```
 *
 * @param rawEmailRequest [SendRawEmailRequest] email 전송 요청 정보
 * @return [SendRawEmailResponse] email 전송 응답 정보
 */
fun SesClient.sendRaw(rawEmailRequest: SendRawEmailRequest): SendRawEmailResponse {
    return sendRawEmail(rawEmailRequest)
}

/**
 * [SendTemplatedEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.sendTemplated(templatedEmailRequest)
 * log.debug { "response=$response" }
 * ```
 *
 * @param templatedEmailRequest [SendTemplatedEmailRequest] email 전송 요청 정보
 * @return [SendTemplatedEmailResponse] email 전송 응답 정보
 */
fun SesClient.sendTemplated(
    templatedEmailRequest: SendTemplatedEmailRequest,
): SendTemplatedEmailResponse {
    return sendTemplatedEmail(templatedEmailRequest)
}

/**
 * [SendBulkTemplatedEmailRequest] 정보를 바탕으로 email을 전송합니다.
 *
 * ```
 * val response = client.sendBulkTemplated(bulkTemplatedEmailRequest)
 * log.debug { "response=$response" }
 * ```
 *
 * @param bulkTemplatedEmailRequest [SendBulkTemplatedEmailRequest] email 전송 요청 정보
 * @return [SendBulkTemplatedEmailResponse] email 전송 응답 정보
 */
fun SesClient.sendBulkTemplated(
    bulkTemplatedEmailRequest: SendBulkTemplatedEmailRequest,
): SendBulkTemplatedEmailResponse {
    return sendBulkTemplatedEmail(bulkTemplatedEmailRequest)
}
