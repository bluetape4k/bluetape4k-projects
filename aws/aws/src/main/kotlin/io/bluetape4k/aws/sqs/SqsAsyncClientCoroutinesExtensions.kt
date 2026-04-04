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

/**
 * 큐를 생성하고 큐 URL을 반환합니다.
 *
 * ```kotlin
 * val queueUrl = client.createQueue("my-queue")
 * // queueUrl.startsWith("http") == true
 * ```
 */
suspend fun SqsAsyncClient.createQueue(queueName: String): String =
    createQueueAsync(queueName).await()

/**
 * 큐 목록을 조회합니다.
 *
 * ```kotlin
 * val response = client.listQueuesSuspend(prefix = "my-")
 * // response.queueUrls().isNotEmpty() == true
 * ```
 */
suspend fun SqsAsyncClient.listQueuesSuspend(
    prefix: String? = null,
    nextToken: String? = null,
    maxResults: Int? = null,
): ListQueuesResponse =
    listQueuesAsync(prefix, nextToken, maxResults).await()

/**
 * 큐 이름으로 큐 URL을 조회합니다.
 *
 * ```kotlin
 * val response = client.getQueueUrl("my-queue")
 * // response.queueUrl().isNotEmpty() == true
 * ```
 */
suspend fun SqsAsyncClient.getQueueUrl(
    queueName: String,
    queueOwnerAWSAccountId: String? = null,
): GetQueueUrlResponse =
    getQueueUrlAsync(queueName, queueOwnerAWSAccountId).await()

/**
 * 메시지를 큐에 전송합니다.
 *
 * ```kotlin
 * val response = client.send(queueUrl, "hello world")
 * // response.messageId().isNotEmpty() == true
 * ```
 */
suspend fun SqsAsyncClient.send(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
): SendMessageResponse =
    sendAsync(queueUrl, messageBody, delaySeconds).await()

/**
 * 여러 메시지를 배치로 큐에 전송합니다.
 *
 * ```kotlin
 * val response = client.sendBatch(queueUrl, entry1, entry2)
 * // response.successful().size == 2
 * ```
 */
suspend fun SqsAsyncClient.sendBatch(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): SendMessageBatchResponse =
    sendBatchAsync(queueUrl, *entries).await()

/**
 * 여러 메시지를 컬렉션으로 배치 전송합니다.
 *
 * ```kotlin
 * val response = client.sendBatch(queueUrl, listOf(entry1, entry2))
 * // response.successful().size == 2
 * ```
 */
suspend fun SqsAsyncClient.sendBatch(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): SendMessageBatchResponse =
    sendBatchAsync(queueUrl, entries).await()

/**
 * 큐에서 메시지를 수신합니다.
 *
 * ```kotlin
 * val response = client.receiveMessages(queueUrl, maxResults = 5)
 * // response.messages().size <= 5
 * ```
 */
suspend fun SqsAsyncClient.receiveMessages(
    queueUrl: String,
    maxResults: Int? = null,
    builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageResponse =
    receiveMessagesAsync(queueUrl, maxResults, builder).await()

/**
 * 메시지의 가시성 타임아웃을 변경합니다.
 *
 * ```kotlin
 * val response = client.changeMessageVisibility(queueUrl, receiptHandle, visibilityTimeout = 30)
 * // response.sdkHttpResponse().isSuccessful == true
 * ```
 */
suspend fun SqsAsyncClient.changeMessageVisibility(
    queueUrl: String,
    receiptHandle: String? = null,
    visibilityTimeout: Int? = null,
): ChangeMessageVisibilityResponse =
    changeMessageVisibilityAsync(queueUrl, receiptHandle, visibilityTimeout).await()

/**
 * 여러 메시지의 가시성 타임아웃을 배치로 변경합니다.
 *
 * ```kotlin
 * val response = client.changeMessageVisibilityBatch(queueUrl, entry1, entry2)
 * // response.successful().size == 2
 * ```
 */
suspend fun SqsAsyncClient.changeMessageVisibilityBatch(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): ChangeMessageVisibilityBatchResponse =
    changeMessageVisibilityBatchAsync(queueUrl, *entries).await()

/**
 * 여러 메시지의 가시성 타임아웃을 컬렉션으로 배치 변경합니다.
 *
 * ```kotlin
 * val response = client.changeMessageVisibilityBatch(queueUrl, listOf(entry1, entry2))
 * // response.successful().size == 2
 * ```
 */
suspend fun SqsAsyncClient.changeMessageVisibilityBatch(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): ChangeMessageVisibilityBatchResponse =
    changeMessageVisibilityBatchAsync(queueUrl, entries).await()

/**
 * 큐에서 메시지를 삭제합니다.
 *
 * ```kotlin
 * val response = client.deleteMessage(queueUrl, receiptHandle)
 * // response.sdkHttpResponse().isSuccessful == true
 * ```
 */
suspend fun SqsAsyncClient.deleteMessage(
    queueUrl: String,
    receiptHandle: String? = null,
): DeleteMessageResponse =
    deleteMessageAsync(queueUrl, receiptHandle).await()

/**
 * 여러 메시지를 배치로 삭제합니다.
 *
 * ```kotlin
 * val response = client.deleteMessageBatch(queueUrl, entry1, entry2)
 * // response.successful().size == 2
 * ```
 */
suspend fun SqsAsyncClient.deleteMessageBatch(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): DeleteMessageBatchResponse =
    deleteMessageBatchAsync(queueUrl, *entries).await()

/**
 * 여러 메시지를 컬렉션으로 배치 삭제합니다.
 *
 * ```kotlin
 * val response = client.deleteMessageBatch(queueUrl, listOf(entry1, entry2))
 * // response.successful().size == 2
 * ```
 */
suspend fun SqsAsyncClient.deleteMessageBatch(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): DeleteMessageBatchResponse =
    deleteMessageBatchAsync(queueUrl, entries).await()

/**
 * 큐를 삭제합니다.
 *
 * ```kotlin
 * val response = client.deleteQueue(queueUrl)
 * // response.sdkHttpResponse().isSuccessful == true
 * ```
 */
suspend fun SqsAsyncClient.deleteQueue(queueUrl: String): DeleteQueueResponse =
    deleteQueueAsync(queueUrl).await()
