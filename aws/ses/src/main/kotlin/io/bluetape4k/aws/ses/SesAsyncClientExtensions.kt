package io.bluetape4k.aws.ses

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.nettyNioAsyncHttpClientOf
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder
import software.amazon.awssdk.services.ses.endpoints.SesEndpointProvider
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailResponse
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse
import java.util.concurrent.CompletableFuture

/**
 * [SesAsyncClient]를 빌드합니다.
 *
 * ```
 * val client = SesAsyncClient {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 *     region(region)
 * }
 * val response = client.send(request).await()
 * ```
 *
 * @param initializer [SesAsyncClientBuilder]를 이용한 초기화 람다
 * @return [SesAsyncClient] 인스턴스
 */
inline fun SesAsyncClient(
    initializer: SesAsyncClientBuilder.() -> Unit,
): SesAsyncClient {
    return SesAsyncClient.builder().apply(initializer).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

/**
 * [SesAsyncClient]를 생성합니다.
 *
 * ```
 * val client = sesAsyncClientOf(region) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * val response = client.send(request).await()
 * ```
 *
 * @param region [Region] 지역
 * @param initializer [SesAsyncClientBuilder]를 이용한 초기화 람다
 * @return [SesAsyncClient] 인스턴스
 */
fun sesAsyncClientOf(
    region: Region,
    initializer: SesAsyncClientBuilder.() -> Unit = {},
): SesAsyncClient = SesAsyncClient {
    region(region)
    httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)

    initializer()
}

/**
 * [SesAsyncClient]를 생성합니다.
 *
 * ```
 * val client = sesAsyncClientOf(endpointProvider) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * val response = client.send(request).await()
 * ```
 *
 * @param endpointProvider [SesEndpointProvider] 엔드포인트 제공자
 * @param initializer [SesAsyncClientBuilder]를 이용한 초기화 람다
 * @return [SesAsyncClient] 인스턴스
 */
fun sesAsyncClientOf(
    endpointProvider: SesEndpointProvider,
    initializer: SesAsyncClientBuilder.() -> Unit = {},
): SesAsyncClient = SesAsyncClient {
    endpointProvider(endpointProvider)
    httpClient(nettyNioAsyncHttpClientOf())

    initializer()
}

/**
 * [SendEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.send(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param emailRequest [SendEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.send(emailRequest: SendEmailRequest): CompletableFuture<SendEmailResponse> {
    return sendEmail(emailRequest)
}

/**
 * [SendRawEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.sendRaw(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param rawEmailRequest [SendRawEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendRawEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.sendRaw(rawEmailRequest: SendRawEmailRequest): CompletableFuture<SendRawEmailResponse> {
    return sendRawEmail(rawEmailRequest)
}

/**
 * [SendTemplatedEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.sendTemplated(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param templatedEmailRequest [SendTemplatedEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendTemplatedEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.sendTemplated(
    templatedEmailRequest: SendTemplatedEmailRequest,
): CompletableFuture<SendTemplatedEmailResponse> {
    return sendTemplatedEmail(templatedEmailRequest)
}

/**
 * [SendBulkTemplatedEmailRequest] 정보를 바탕으로 email을 비동기로 전송합니다.
 *
 * ```
 * val response = client.sendBulkTemplated(request).await()
 * response.messageId().shouldNotBeEmpty()
 * log.debug { "response=$response" }
 * ```
 *
 * @param bulkTemplatedEmailRequest [SendBulkTemplatedEmailRequest] email 전송 요청 정보
 * @return [CompletableFuture]<[SendBulkTemplatedEmailResponse]> email 전송 응답 정보
 */
fun SesAsyncClient.sendBulkTemplated(
    bulkTemplatedEmailRequest: SendBulkTemplatedEmailRequest,
): CompletableFuture<SendBulkTemplatedEmailResponse> {
    return sendBulkTemplatedEmail(bulkTemplatedEmailRequest)
}
