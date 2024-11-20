package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.aws.sqs.model.sendMessageRequestOf
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
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

inline fun sqsClient(initializer: SqsClientBuilder.() -> Unit): SqsClient {
    return SqsClient.builder().apply(initializer).build()
}

fun sqsClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    initializer: SqsClientBuilder.() -> Unit = {},
): SqsClient = sqsClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)

    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)
    initializer()
}

fun SqsClient.createQueue(queueName: String): String {
    queueName.requireNotBlank("queueName")
    return createQueue { it.queueName(queueName) }.queueUrl()
}

fun SqsClient.listQueues(
    prefix: String? = null,
    nextToken: String? = null,
    maxResults: Int? = null,
): ListQueuesResponse {
    return listQueues {
        prefix?.run { it.queueNamePrefix(prefix) }
        nextToken?.run { it.nextToken(nextToken) }
        maxResults?.run { it.maxResults(maxResults) }
    }
}

fun SqsClient.getQueueUrl(queueName: String): GetQueueUrlResponse {
    queueName.requireNotBlank("queueName")
    return getQueueUrl { it.queueName(queueName) }
}

fun SqsClient.send(queueUrl: String, messageBody: String): SendMessageResponse {
    return sendMessage(sendMessageRequestOf(queueUrl, messageBody))
}

fun SqsClient.sendBatch(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsClient.sendBatch(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    return sendMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsClient.receiveMessages(
    queueUrl: String,
    maxResults: Int? = null,
    requestInitializer: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageResponse {
    queueUrl.requireNotBlank("queueUrl")
    return receiveMessage {
        it.queueUrl(queueUrl)
        maxResults?.run { it.maxNumberOfMessages(this) }
        it.requestInitializer()
    }
}

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

fun SqsClient.changeMessageVisibilityBatch(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsClient.changeMessageVisibilityBatch(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    return changeMessageVisibilityBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

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

fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(*entries)
    }
}

fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    return deleteMessageBatch {
        it.queueUrl(queueUrl)
        it.entries(entries)
    }
}

fun SqsClient.deleteQueue(queueUrl: String): DeleteQueueResponse {
    queueUrl.requireNotBlank("queueUrl")
    return deleteQueue {
        it.queueUrl(queueUrl)
    }
}
