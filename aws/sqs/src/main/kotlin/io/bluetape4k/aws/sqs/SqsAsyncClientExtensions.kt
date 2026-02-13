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

/**
 * Create [SqsAsyncClient] instance
 * 사용 후에는 꼭 `close()`를 호출하거나 , `use` 를 사용해서 cleanup 해주어야 합니다.
 */
inline fun sqsAsyncClient(
    @BuilderInference builder: SqsAsyncClientBuilder.() -> Unit,
): SqsAsyncClient {
    return SqsAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

inline fun sqsAsyncClientOf(
    region: Region,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient,
    @BuilderInference builder: SqsAsyncClientBuilder.() -> Unit = {},
): SqsAsyncClient = sqsAsyncClient {
    region(region)
    httpClient(httpClient)

    builder()
}

inline fun sqsAsyncClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient,
    @BuilderInference builder: SqsAsyncClientBuilder.() -> Unit = {},
): SqsAsyncClient = sqsAsyncClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
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
): CompletableFuture<ListQueuesResponse> {
    return listQueues {
        prefix?.run { it.queueNamePrefix(this) }
        nextToken?.run { it.nextToken(this) }
        maxResults?.run { it.maxResults(this) }
    }
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

fun SqsAsyncClient.sendBatchAsync(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): CompletableFuture<SendMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsAsyncClient.sendBatchAsync(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): CompletableFuture<SendMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsAsyncClient.receiveMessagesAsync(
    queueUrl: String,
    maxResults: Int? = null,
    @BuilderInference builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): CompletableFuture<ReceiveMessageResponse> {
    queueUrl.requireNotBlank("queueUrl")
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

fun SqsAsyncClient.changeMessageVisibilityBatchAsync(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): CompletableFuture<ChangeMessageVisibilityBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsAsyncClient.changeMessageVisibilityBatchAsync(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): CompletableFuture<ChangeMessageVisibilityBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
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

fun SqsAsyncClient.deleteMessageBatchAsync(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): CompletableFuture<DeleteMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsAsyncClient.deleteMessageBatchAsync(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): CompletableFuture<DeleteMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
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
