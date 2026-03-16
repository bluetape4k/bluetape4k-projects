package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.sqs.model.sendMessageRequestOf
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder
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
import java.util.concurrent.CompletableFuture

private const val MIN_RECEIVE_MESSAGES = 1
private const val MAX_RECEIVE_MESSAGES = 10

/**
 * DSL 빌더로 [SqsAsyncClient]를 생성합니다.
 *
 * 생성된 클라이언트는 JVM 종료 시 자동으로 닫히도록 [ShutdownQueue]에 등록됩니다.
 */
inline fun sqsAsyncClient(
    @BuilderInference builder: SqsAsyncClientBuilder.() -> Unit,
): SqsAsyncClient =
    SqsAsyncClient
        .builder()
        .apply(builder)
        .build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * 편의 파라미터로 [SqsAsyncClient]를 생성합니다.
 *
 * @param endpoint 엔드포인트 URI (LocalStack 등 오버라이드 시 지정)
 * @param region AWS 리전
 * @param credentialsProvider 자격증명 공급자
 * @param httpClient 비동기 HTTP 클라이언트
 */
inline fun sqsAsyncClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.Netty.httpClient,
    @BuilderInference builder: SqsAsyncClientBuilder.() -> Unit = {},
): SqsAsyncClient =
    sqsAsyncClient {
        endpoint?.let { endpointOverride(it) }
        region?.let { region(it) }
        credentialsProvider?.let { credentialsProvider(it) }
        httpClient(httpClient)

        builder()
    }

fun SqsAsyncClient.createQueueAsync(queueName: String): CompletableFuture<String> {
    queueName.requireNotBlank("queueName")
    return createQueue { it.queueName(queueName) }
        .thenApply { it.queueUrl() }
}

fun SqsAsyncClient.listQueuesAsync(
    prefix: String? = null,
    nextToken: String? = null,
    maxResults: Int? = null,
): CompletableFuture<ListQueuesResponse> =
    listQueues {
        prefix?.run { it.queueNamePrefix(this) }
        nextToken?.run { it.nextToken(this) }
        maxResults?.run { it.maxResults(this) }
    }

fun SqsAsyncClient.getQueueUrlAsync(
    queueName: String,
    queueOwnerAWSAccountId: String? = null,
): CompletableFuture<GetQueueUrlResponse> {
    queueName.requireNotBlank("queueName")
    return getQueueUrl {
        it.queueName(queueName)
        queueOwnerAWSAccountId?.run { it.queueOwnerAWSAccountId(this) }
    }
}

fun SqsAsyncClient.sendAsync(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
): CompletableFuture<SendMessageResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessage(sendMessageRequestOf(queueUrl, messageBody, delaySeconds))
}

/**
 * 메시지를 배치로 비동기 전송합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsAsyncClient.sendBatchAsync(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): CompletableFuture<SendMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

/**
 * 메시지를 배치로 비동기 전송합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsAsyncClient.sendBatchAsync(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): CompletableFuture<SendMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

/**
 * 큐에서 메시지를 비동기로 조회합니다.
 *
 * [maxResults]를 지정하면 SQS 제약(1..10)을 선검증해 네트워크 호출 전에 실패합니다.
 */
fun SqsAsyncClient.receiveMessagesAsync(
    queueUrl: String,
    maxResults: Int? = null,
    @BuilderInference builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): CompletableFuture<ReceiveMessageResponse> {
    queueUrl.requireNotBlank("queueUrl")
    maxResults?.let { validateReceiveMessageCount(it) }
    return receiveMessage {
        it.queueUrl(queueUrl)
        maxResults?.run { it.maxNumberOfMessages(this) }
        it.builder()
    }
}

fun SqsAsyncClient.changeMessageVisibilityAsync(
    queueUrl: String,
    receiptHandle: String? = null,
    visibilityTimeout: Int? = null,
): CompletableFuture<ChangeMessageVisibilityResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return changeMessageVisibility {
        it.queueUrl(queueUrl)
        receiptHandle?.run { it.receiptHandle(this) }
        visibilityTimeout?.run { it.visibilityTimeout(this) }
    }
}

/**
 * 메시지 가시성 타임아웃을 배치로 비동기 변경합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsAsyncClient.changeMessageVisibilityBatchAsync(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): CompletableFuture<ChangeMessageVisibilityBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

/**
 * 메시지 가시성 타임아웃을 배치로 비동기 변경합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsAsyncClient.changeMessageVisibilityBatchAsync(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): CompletableFuture<ChangeMessageVisibilityBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsAsyncClient.deleteMessageAsync(
    queueUrl: String,
    receiptHandle: String? = null,
): CompletableFuture<DeleteMessageResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessage {
        it.queueUrl(queueUrl)
        receiptHandle?.run { it.receiptHandle(this) }
    }
}

/**
 * 메시지를 배치로 비동기 삭제합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsAsyncClient.deleteMessageBatchAsync(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): CompletableFuture<DeleteMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

/**
 * 메시지를 배치로 비동기 삭제합니다.
 *
 * [entries]가 비어 있으면 네트워크 호출 전에 [IllegalArgumentException]을 던집니다.
 */
fun SqsAsyncClient.deleteMessageBatchAsync(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): CompletableFuture<DeleteMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    require(entries.isNotEmpty()) { "entries must not be empty" }
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsAsyncClient.deleteQueueAsync(queueUrl: String): CompletableFuture<DeleteQueueResponse> {
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
