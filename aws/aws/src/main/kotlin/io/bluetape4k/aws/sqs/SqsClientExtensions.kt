package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.aws.sqs.model.sendMessageRequestOf
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.SqsClientBuilder
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.net.URI

private const val MIN_RECEIVE_MESSAGES = 1
private const val MAX_RECEIVE_MESSAGES = 10

/**
 * DSL 빌더로 [SqsClient]를 생성합니다.
 *
 * 생성된 클라이언트는 JVM 종료 시 자동으로 닫히도록 [ShutdownQueue]에 등록됩니다.
 *
 * ```kotlin
 * val client = sqsClient { region(Region.AP_NORTHEAST_2) }
 * // client != null
 * ```
 */
inline fun sqsClient(
    builder: SqsClientBuilder.() -> Unit,
): SqsClient =
    SqsClient
        .builder()
        .apply(builder)
        .build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * 편의 파라미터로 [SqsClient]를 생성합니다.
 *
 * @param endpoint 엔드포인트 URI (LocalStack 등 오버라이드 시 지정)
 * @param region AWS 리전
 * @param credentialsProvider 자격증명 공급자
 * @param httpClient HTTP 클라이언트
 *
 * ```kotlin
 * val client = sqsClientOf(endpoint = URI("http://localhost:4566"), region = Region.AP_NORTHEAST_2)
 * // client != null
 * ```
 */
inline fun sqsClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    builder: SqsClientBuilder.() -> Unit = {},
): SqsClient =
    sqsClient {
        endpoint?.let { endpointOverride(it) }
        region?.let { region(it) }
        credentialsProvider?.let { credentialsProvider(it) }
        httpClient(httpClient)

        builder()
    }

/**
 * [queueName]으로 SQS 큐를 생성하고 큐 URL을 반환합니다.
 *
 * ```kotlin
 * val queueUrl = sqsClient.createQueue("my-queue")
 * // queueUrl.contains("my-queue") == true
 * ```
 */
fun SqsClient.createQueue(queueName: String): String {
    queueName.requireNotBlank("queueName")
    return createQueue { it.queueName(queueName) }.queueUrl()
}

/**
 * SQS 큐 목록을 조회합니다.
 *
 * ```kotlin
 * val response = sqsClient.listQueues(prefix = "my-")
 * // response.queueUrls().isNotEmpty() == true
 * ```
 */
fun SqsClient.listQueues(
    prefix: String? = null,
    nextToken: String? = null,
    maxResults: Int? = null,
): ListQueuesResponse =
    listQueues {
        prefix?.run { it.queueNamePrefix(prefix) }
        nextToken?.run { it.nextToken(nextToken) }
        maxResults?.run { it.maxResults(maxResults) }
    }

/**
 * [queueName]으로 SQS 큐 URL을 조회합니다.
 *
 * ```kotlin
 * val response = sqsClient.getQueueUrl("my-queue")
 * // response.queueUrl().contains("my-queue") == true
 * ```
 */
fun SqsClient.getQueueUrl(queueName: String): GetQueueUrlResponse {
    queueName.requireNotBlank("queueName")
    return getQueueUrl { it.queueName(queueName) }
}

/**
 * [queueUrl] 큐에 [messageBody]를 전송합니다.
 *
 * ```kotlin
 * val response = sqsClient.send("https://sqs.ap-northeast-2.amazonaws.com/123/my-queue", "hello")
 * // response.messageId().isNotBlank() == true
 * ```
 */
fun SqsClient.send(
    queueUrl: String,
    messageBody: String,
): SendMessageResponse = sendMessage(sendMessageRequestOf(queueUrl, messageBody))

/**
 * 메시지를 배치로 전송합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsClient.sendBatch(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

/**
 * 메시지를 배치로 전송합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsClient.sendBatch(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

/**
 * 큐에서 메시지를 조회합니다.
 *
 * [maxResults]를 지정하면 SQS 제약(1..10)을 선검증해 네트워크 호출 전에 실패합니다.
 */
fun SqsClient.receiveMessages(
    queueUrl: String,
    maxResults: Int? = null,
    builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageResponse {
    queueUrl.requireNotBlank("queueUrl")
    maxResults?.let { validateReceiveMessageCount(it) }

    return receiveMessage {
        it.queueUrl(queueUrl)
        maxResults?.run { it.maxNumberOfMessages(this) }
        it.builder()
    }
}

/**
 * 큐 메시지의 가시성 타임아웃을 변경합니다.
 *
 * ```kotlin
 * val response = sqsClient.changeMessageVisibility(queueUrl, receiptHandle = handle, visibilityTimeout = 30)
 * // response.sdkHttpResponse().isSuccessful == true
 * ```
 */
fun SqsClient.changeMessageVisibility(
    queueUrl: String,
    receiptHandle: String? = null,
    visibilityTimeout: Int? = null,
): ChangeMessageVisibilityResponse {
    queueUrl.requireNotBlank("queueUrl")
    return changeMessageVisibility {
        it.queueUrl(queueUrl)
        receiptHandle?.run { it.receiptHandle(this) }
        visibilityTimeout?.run { it.visibilityTimeout(this) }
    }
}

/**
 * 수신된 메시지들의 가시성 타임아웃을 일괄 변경합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsClient.changeMessageVisibilityBatch(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

/**
 * 수신된 메시지들의 가시성 타임아웃을 일괄 변경합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsClient.changeMessageVisibilityBatch(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

/**
 * 큐에서 메시지를 삭제합니다.
 *
 * ```kotlin
 * val response = sqsClient.deleteMessage(queueUrl, receiptHandle = handle)
 * // response.sdkHttpResponse().isSuccessful == true
 * ```
 */
fun SqsClient.deleteMessage(
    queueUrl: String,
    receiptHandle: String? = null,
): DeleteMessageResponse {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessage {
        it.queueUrl(queueUrl)
        receiptHandle?.run { it.receiptHandle(this) }
    }
}

/**
 * 여러 메시지를 일괄 삭제합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

/**
 * 여러 메시지를 일괄 삭제합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

/**
 * SQS 큐를 삭제합니다.
 *
 * ```kotlin
 * val response = sqsClient.deleteQueue("https://sqs.ap-northeast-2.amazonaws.com/123/my-queue")
 * // response.sdkHttpResponse().isSuccessful == true
 * ```
 */
fun SqsClient.deleteQueue(queueUrl: String): DeleteQueueResponse {
    queueUrl.requireNotBlank("queueUrl")

    return deleteQueue {
        it.queueUrl(queueUrl)
    }
}

private fun validateReceiveMessageCount(maxResults: Int) {
    require(maxResults in MIN_RECEIVE_MESSAGES..MAX_RECEIVE_MESSAGES) {
        "maxResults must be in $MIN_RECEIVE_MESSAGES..$MAX_RECEIVE_MESSAGES, but was $maxResults"
    }
}
