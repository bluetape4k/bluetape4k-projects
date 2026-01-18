package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.sqs.model.sendMessageRequestOf
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
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
inline fun sqsAsyncClient(builder: SqsAsyncClientBuilder.() -> Unit): SqsAsyncClient {
    return SqsAsyncClient.builder().apply(builder).build()
}

inline fun sqsAsyncClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    builder: SqsAsyncClientBuilder.() -> Unit = {},
): SqsAsyncClient = sqsAsyncClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)

    httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)

    builder()
}

fun SqsAsyncClient.createQueue(queueName: String): CompletableFuture<String> {
    queueName.requireNotBlank("queueName")
    return createQueue { it.queueName(queueName) }
        .thenApply { it.queueUrl() }
}

fun SqsAsyncClient.listQueues(
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

fun SqsAsyncClient.getQueueUrl(
    queueName: String,
    queueOwnerAWSAccountId: String? = null,
): CompletableFuture<GetQueueUrlResponse> {
    queueName.requireNotBlank("queueName")
    return getQueueUrl {
        it.queueName(queueName)
        queueOwnerAWSAccountId?.run { it.queueOwnerAWSAccountId(this) }
    }
}

fun SqsAsyncClient.send(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
): CompletableFuture<SendMessageResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessage(sendMessageRequestOf(queueUrl, messageBody, delaySeconds))
}

fun SqsAsyncClient.sendBatch(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): CompletableFuture<SendMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsAsyncClient.sendBatch(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): CompletableFuture<SendMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsAsyncClient.receiveMessages(
    queueUrl: String,
    maxResults: Int? = null,
    requestInitializer: ReceiveMessageRequest.Builder.() -> Unit = {},
): CompletableFuture<ReceiveMessageResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return receiveMessage {
        it.queueUrl(queueUrl)
        maxResults?.run { it.maxNumberOfMessages(this) }
        it.requestInitializer()
    }
}

fun SqsAsyncClient.changeMessageVisibility(
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

fun SqsAsyncClient.changeMessageVisibilityBatch(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): CompletableFuture<ChangeMessageVisibilityBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsAsyncClient.changeMessageVisibilityBatch(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): CompletableFuture<ChangeMessageVisibilityBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsAsyncClient.deleteMessage(
    queueUrl: String,
    receiptHandle: String? = null,
): CompletableFuture<DeleteMessageResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessage {
        it.queueUrl(queueUrl)
        receiptHandle?.run { it.receiptHandle(this) }
    }
}

fun SqsAsyncClient.deleteMessageBatch(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): CompletableFuture<DeleteMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsAsyncClient.deleteMessageBatch(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): CompletableFuture<DeleteMessageBatchResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsAsyncClient.deleteQueue(queueUrl: String): CompletableFuture<DeleteQueueResponse> {
    queueUrl.requireNotBlank("queueUrl")
    return deleteQueue {
        it.queueUrl(queueUrl)
    }
}
