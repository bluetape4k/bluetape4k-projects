package io.bluetape4k.aws.sqs

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
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

suspend fun SqsAsyncClient.createQueue(queueName: String): String =
    createQueueAsync(queueName).await()

suspend fun SqsAsyncClient.listQueuesSuspend(
    prefix: String? = null,
    nextToken: String? = null,
    maxResults: Int? = null,
): ListQueuesResponse =
    listQueuesAsync(prefix, nextToken, maxResults).await()

suspend fun SqsAsyncClient.getQueueUrl(
    queueName: String,
    queueOwnerAWSAccountId: String? = null,
): GetQueueUrlResponse =
    getQueueUrlAsync(queueName, queueOwnerAWSAccountId).await()

suspend fun SqsAsyncClient.send(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
): SendMessageResponse =
    sendAsync(queueUrl, messageBody, delaySeconds).await()

suspend fun SqsAsyncClient.sendBatch(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): SendMessageBatchResponse =
    sendBatchAsync(queueUrl, *entries).await()

suspend fun SqsAsyncClient.sendBatch(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): SendMessageBatchResponse =
    sendBatchAsync(queueUrl, entries).await()

suspend fun SqsAsyncClient.receiveMessages(
    queueUrl: String,
    maxResults: Int? = null,
    @BuilderInference builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageResponse =
    receiveMessagesAsync(queueUrl, maxResults, builder).await()

suspend fun SqsAsyncClient.changeMessageVisibility(
    queueUrl: String,
    receiptHandle: String? = null,
    visibilityTimeout: Int? = null,
): ChangeMessageVisibilityResponse =
    changeMessageVisibilityAsync(queueUrl, receiptHandle, visibilityTimeout).await()

suspend fun SqsAsyncClient.changeMessageVisibilityBatch(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): ChangeMessageVisibilityBatchResponse =
    changeMessageVisibilityBatchAsync(queueUrl, *entries).await()

suspend fun SqsAsyncClient.changeMessageVisibilityBatch(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): ChangeMessageVisibilityBatchResponse =
    changeMessageVisibilityBatchAsync(queueUrl, entries).await()

suspend fun SqsAsyncClient.deleteMessage(
    queueUrl: String,
    receiptHandle: String? = null,
): DeleteMessageResponse =
    deleteMessageAsync(queueUrl, receiptHandle).await()

suspend fun SqsAsyncClient.deleteMessageBatch(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): DeleteMessageBatchResponse =
    deleteMessageBatchAsync(queueUrl, *entries).await()

suspend fun SqsAsyncClient.deleteMessageBatch(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): DeleteMessageBatchResponse =
    deleteMessageBatchAsync(queueUrl, entries).await()

suspend fun SqsAsyncClient.deleteQueue(queueUrl: String): DeleteQueueResponse =
    deleteQueueAsync(queueUrl).await()
