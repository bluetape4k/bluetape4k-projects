package io.bluetape4k.aws.kotlin.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.changeMessageVisibility
import aws.sdk.kotlin.services.sqs.changeMessageVisibilityBatch
import aws.sdk.kotlin.services.sqs.createQueue
import aws.sdk.kotlin.services.sqs.deleteMessage
import aws.sdk.kotlin.services.sqs.deleteMessageBatch
import aws.sdk.kotlin.services.sqs.deleteQueue
import aws.sdk.kotlin.services.sqs.getQueueUrl
import aws.sdk.kotlin.services.sqs.listQueues
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityBatchResponse
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityResponse
import aws.sdk.kotlin.services.sqs.model.CreateQueueRequest
import aws.sdk.kotlin.services.sqs.model.CreateQueueResponse
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchResponse
import aws.sdk.kotlin.services.sqs.model.DeleteMessageResponse
import aws.sdk.kotlin.services.sqs.model.DeleteQueueRequest
import aws.sdk.kotlin.services.sqs.model.DeleteQueueResponse
import aws.sdk.kotlin.services.sqs.model.GetQueueUrlRequest
import aws.sdk.kotlin.services.sqs.model.ListQueuesRequest
import aws.sdk.kotlin.services.sqs.model.ListQueuesResponse
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageResponse
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchResponse
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageResponse
import aws.sdk.kotlin.services.sqs.receiveMessage
import aws.sdk.kotlin.services.sqs.sendMessage
import aws.sdk.kotlin.services.sqs.sendMessageBatch
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.utils.ShutdownQueue

val log by lazy { KotlinLogging.logger { } }
@PublishedApi
internal const val MIN_RECEIVE_MESSAGES = 1

@PublishedApi
internal const val MAX_RECEIVE_MESSAGES = 10

/**
 * [SqsClient] 인스턴스를 생성합니다.
 *
 * ```
 * val sqsClient = sqsClientOf(
 *      endpoint = "http://localhost:4566",
 *      region = "us-east-1",
 *      credentialsProvider = credentialsProvider
 * ) {
 *      // 설정
 *      retryPolicy {
 *      }
 *      interpreter {
 *      }
 * }
 * ```
 *
 * @param endpointUrl Amazon SQS endpoint URL입니다.
 * @param region AWS region입니다.
 * @param credentialsProvider AWS credentials provider입니다.
 * @param httpClient [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param builder Amazon SQS client 설정 빌더입니다.
 * @return [SqsClient] 인스턴스를 반환합니다.
 */
inline fun sqsClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine = HttpClientEngineProvider.defaultHttpEngine,
    @BuilderInference crossinline builder: SqsClient.Config.Builder.() -> Unit = {},
): SqsClient {
    endpointUrl?.hostAndPort.requireNotBlank("endpointUrl")

    return SqsClient {
        endpointUrl?.let { this.endpointUrl = it }
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }

        this.httpClient = httpClient

        builder()
    }.apply {
        ShutdownQueue.register(this)
    }
}

/**
 * [queueName]에 해당하는 큐를 생성합니다.
 *
 * ```
 * val response = sqsClient.createQueue("my-queue")
 * ```
 *
 * @param queueName Amazon SQS 큐의 이름입니다.
 * @return CreateQueueResponse 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.createQueue(
    queueName: String,
    @BuilderInference crossinline builder: CreateQueueRequest.Builder.() -> Unit = {},
): CreateQueueResponse {
    queueName.requireNotBlank("queueName")

    return createQueue {
        this.queueName = queueName
        builder()
    }.apply {
        log.info { "Create Queue. response=$this" }
    }
}

/**
 * [queueName]에 해당하는 큐가 존재하지 않으면 큐를 생성합니다.
 *
 * ```
 * val queueUrl = sqsClient.ensureQueue("my-queue")
 * ```
 *
 * @param queueName Amazon SQS 큐의 이름입니다.
 * @return 큐의 URL을 반환합니다.
 * @see [existsQueue]
 */
suspend inline fun SqsClient.ensureQueue(
    queueName: String,
    @BuilderInference crossinline builder: CreateQueueRequest.Builder.() -> Unit = {},
): String? {
    queueName.requireNotBlank("queueName")

    if (!existsQueue(queueName)) {
        createQueue(queueName, builder)
    }
    return getQueueUrl(queueName)
}

/**
 * [queueName]에 해당하는 큐가 존재하는지 확인합니다.
 *
 * ```
 * val exists = sqsClient.existsQueue("my-queue")
 * ```
 *
 * @param queueName Amazon SQS 큐의 이름입니다.
 * @return 큐가 존재하면 true, 그렇지 않으면 false를 반환합니다.
 */
suspend inline fun SqsClient.existsQueue(queueName: String): Boolean = runCatching {
    getQueueUrl(queueName)?.isNotBlank() ?: false
}.getOrDefault(false)

/**
 * [queueNamePrefix]를 접두사로 가지는 큐 목록을 반환합니다.
 *
 * ```
 * val response = sqsClient.listQueues(queueNamePrefix = "my-queue")
 * val queueUrls = response.queueUrls
 * ```
 *
 * @param queueNamePrefix Amazon SQS 큐의 이름 접두사입니다.
 * @param nextToken 다음 페이지의 토큰입니다. 기본값은 null입니다.
 * @param maxResults 반환할 최대 결과 수입니다. 기본값은 null입니다.
 * @return ListQueuesResponse 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.listQueues(
    queueNamePrefix: String,
    nextToken: String? = null,
    maxResults: Int? = null,
    @BuilderInference crossinline builder: ListQueuesRequest.Builder.() -> Unit = {},
): ListQueuesResponse {
    queueNamePrefix.requireNotBlank("queueNamePrefix")

    return listQueues {
        this.queueNamePrefix = queueNamePrefix
        nextToken?.let { this.nextToken = it }
        maxResults?.let { this.maxResults = it }

        builder()
    }
}

/**
 * [queueName]에 해당하는 큐의 URL을 반환합니다.
 *
 * ```
 * val response = sqsClient.getQueueUrl("my-queue")
 * val queueUrl = response.queueUrl
 * ```
 *
 * @param queueName Amazon SQS 큐의 이름입니다.
 * @return 해당 큐의 URL을 반환한다. 해당 큐가 없으면 예외가 발생합니다.
 */
suspend inline fun SqsClient.getQueueUrl(
    queueName: String,
    @BuilderInference crossinline builder: GetQueueUrlRequest.Builder.() -> Unit = {},
): String? {
    queueName.requireNotBlank("queueName")

    return getQueueUrl {
        this.queueName = queueName
        builder()
    }.queueUrl
}

/**
 * [queueUrl]에 해당하는 큐를 삭제합니다.
 *
 * ```
 * val response = sqsClient.deleteQueue("https://sqs.ap-northeast-2.amazonaws.com/123456789012/my-queue")
 * ```
 *
 * @param queueUrl Amazon SQS 큐의 URL입니다.
 * @return DeleteQueueResponse 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.deleteQueue(
    queueUrl: String,
    @BuilderInference crossinline builder: DeleteQueueRequest.Builder.() -> Unit = {},
): DeleteQueueResponse {
    queueUrl.requireNotBlank("queueUrl")

    return deleteQueue {
        this.queueUrl = queueUrl
        builder()
    }
}


/**
 * AWS SQS 에 메시지를 보냅니다.
 *
 * ```
 * val response = sqsClient.send(
 *      queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue",
 *      messageBody = "Hello, World!",
 *      delaySeconds = 10,
 * ) {
 *      messageAttributes = mapOf("key" to messageValueAttributeOf("value"))
 * }
 *
 * @param queueUrl 메시지를 보낼 Amazon SQS 큐의 URL입니다.
 * @param messageBody 전송할 메시지의 본문입니다. blank이면 [IllegalArgumentException]을 던집니다.
 * @param delaySeconds 메시지를 보내기 전 대기할 시간(초)입니다. 기본값은 null입니다.
 * @param builder SendMessageRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 * @return SendMessageResponse 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.sendMessage(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
    @BuilderInference crossinline builder: SendMessageRequest.Builder.() -> Unit = {},
): SendMessageResponse {
    queueUrl.requireNotBlank("queueUrl")
    messageBody.requireNotBlank("messageBody")

    return sendMessage {
        this.queueUrl = queueUrl
        this.messageBody = messageBody
        delaySeconds?.let { this.delaySeconds = it }

        builder()
    }
}

/**
 * AWS SQS 에 배치로 메시지를 보냅니다.
 *
 * ```
 * val entry1 = SendMessageBatchRequestEntry { id="id1"; messageBody="Hello, World!" }
 * val entry2 = SendMessageBatchRequestEntry { id="id2"; messageBody="Hello, World!" }
 *
 * val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"
 *
 * val response = sqsClient.sendBatch(queueUrl, entry1, entry2)
 * ```
 *
 * @param queueUrl 메시지를 보낼 Amazon SQS 큐의 URL입니다.
 * @param entries 전송할 메시지의 목록입니다.
 * @return SendMessageBatchResponse 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.sendMessageBatch(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    entries.requireNotEmpty("entries")

    return sendMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toList()
    }
}

/**
 * AWS SQS 에 배치로 메시지를 보냅니다.
 *
 * ```
 * val entry1 = SendMessageBatchRequestEntry { id="id1"; messageBody="Hello, World!" }
 * val entry2 = SendMessageBatchRequestEntry { id="id2"; messageBody="Hello, World!" }
 * val entries = listOf(entry1, entry2)
 *
 * val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"
 *
 * val response = sqsClient.sendBatch(queueUrl, entries)
 * ```
 *
 * @param queueUrl 메시지를 보낼 Amazon SQS 큐의 URL입니다.
 * @param entries 전송할 메시지의 목록입니다.
 * @return SendMessageBatchResponse 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.sendMessageBatch(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    entries.requireNotEmpty("entries")

    return sendMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toList()
    }
}

/**
 * 제공된 queueUrl을 사용하여 Amazon SQS 큐에서 메시지를 수신합니다.
 *
 * ```
 * val response = sqsClient.receive(queueUrl, maxResults = 10)
 * val messages = response.messages
 * ```
 *
 * @param queueUrl 메시지를 수신할 Amazon SQS 큐의 URL입니다.
 * @param maxNumberOfMessages 한 번에 수신할 최대 메시지 수입니다. 기본값은 null이며, 지정 시 1..10 범위여야 합니다.
 */
suspend inline fun SqsClient.receiveMessage(
    queueUrl: String,
    maxNumberOfMessages: Int? = null,
): ReceiveMessageResponse {
    queueUrl.requireNotBlank("queueUrl")
    maxNumberOfMessages?.let {
        require(it in MIN_RECEIVE_MESSAGES..MAX_RECEIVE_MESSAGES) {
            "maxNumberOfMessages must be in the range $MIN_RECEIVE_MESSAGES..$MAX_RECEIVE_MESSAGES."
        }
    }

    return receiveMessage {
        this.queueUrl = queueUrl
        maxNumberOfMessages?.let { this.maxNumberOfMessages = it }
    }
}


/**
 * [queueUrl]의 [receiptHandle]을 가진 메시지에 대해 Visibility를 변경합니다.
 *
 * ```
 * val response = sqsClient.changeMessageVisibility(
 *     queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue",
 *     receiptHandle = "receiptHandle",
 *     visibilityTimeout = 10
 * ) {
 * ```
 *
 * @param queueUrl 메시지의 Visibility를 변경할 Amazon SQS 큐의 URL입니다.
 * @param receiptHandle Visibility를 변경할 메시지와 연관된 영수증 핸들입니다.
 * @param visibilityTimeout 메시지의 새로운 VisibilityTimeout(초)입니다. 기본값은 null입니다.
 *
 * @return ChangeMessageVisibilityResponse 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.changeMessageVisibility(
    queueUrl: String,
    receiptHandle: String? = null,
    visibilityTimeout: Int? = null,
): ChangeMessageVisibilityResponse {
    queueUrl.requireNotBlank("queueUrl")

    return changeMessageVisibility {
        this.queueUrl = queueUrl
        receiptHandle?.let { this.receiptHandle = it }
        visibilityTimeout?.let { this.visibilityTimeout = it }
    }
}

/**
 * [queueUrl]의 [entries]에 대해 Batch 방식으로 Visibility를 변경합니다.
 *
 * ```
 * val entry1 = ChangeMessageVisibilityBatchRequestEntry {
 *     id = "id1"
 *     receiptHandle = "receiptHandle1"
 *     visibilityTimeout = 10
 * }
 * val entry2 = ChangeMessageVisibilityBatchRequestEntry {
 *      id = "id2"
 *      receiptHandle = "receiptHandle2"
 *      visibilityTimeout = 20
 * }
 * val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"
 *
 * val response = sqsClient.changeVisibilityBatch(queueUrl, entry1, entry2)
 * ```
 *
 * @param queueUrl 메시지의 Visibility를 변경할 Amazon SQS 큐의 URL입니다.
 * @param entries ChangeMessageVisibilityBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return [ChangeMessageVisibilityBatchResponse] 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.changeMessageVisibilityBatch(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    entries.requireNotEmpty("entries")

    return changeMessageVisibilityBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toList()
    }
}

/**
 * [queueUrl]의 [entries]에 대해 Batch 방식으로 Visibility를 변경합니다.
 *
 * ```
 * val entry1 = ChangeMessageVisibilityBatchRequestEntry {
 *     id = "id1"
 *     receiptHandle = "receiptHandle1"
 *     visibilityTimeout = 10
 * }
 * val entry2 = ChangeMessageVisibilityBatchRequestEntry {
 *      id = "id2"
 *      receiptHandle = "receiptHandle2"
 *      visibilityTimeout = 20
 * }
 * val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"
 *
 * val response = sqsClient.changeVisibilityBatch(queueUrl, listOf(entry1, entry2))
 * ```
 *
 * @param queueUrl 메시지의 Visibility를 변경할 Amazon SQS 큐의 URL입니다.
 * @param entries ChangeMessageVisibilityBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return [ChangeMessageVisibilityBatchResponse] 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.changeMessageVisibilityBatch(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    entries.requireNotEmpty("entries")

    return changeMessageVisibilityBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toList()
    }
}


/**
 * 제공된 queueUrl과 receiptHandle을 사용하여 메시지를 삭제합니다.
 *
 * ```
 * val response = sqsClient.deleteMessage(
 *      "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue",
 *      "receiptHandle"
 * )
 * ```
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param receiptHandle 삭제할 메시지와 연관된 영수증 핸들입니다.
 * @return [DeleteMessageResponse] 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.deleteMessage(
    queueUrl: String,
    receiptHandle: String? = null,
): DeleteMessageResponse {
    queueUrl.requireNotBlank("queueUrl")

    return deleteMessage {
        this.queueUrl = queueUrl
        receiptHandle?.let { this.receiptHandle = it }
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 메시지를 배치로 삭제합니다.
 *
 * ```
 * val response = sqsClient.deleteMessageBatch(
 *     "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue",
 *     deleteMessageBatchRequestEntryOf("id1", "receiptHandle1"),
 *     deleteMessageBatchRequestEntryOf("id2", "receiptHandle2"),
 * )
 * ```
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param entries DeleteMessageBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return [DeleteMessageBatchResponse] 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    entries.requireNotEmpty("entries")

    return deleteMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toList()
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 메시지를 배치로 삭제합니다.
 *
 * ```
 * val entries = listOf(
 *      deleteMessageBatchRequestEntryOf("id1", "receiptHandle1"),
 *      deleteMessageBatchRequestEntryOf("id2", "receiptHandle2"),
 *      ...
 * )
 * val response = sqsClient.deleteMessageBatch(
 *    "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue",
 *    entries
 * )
 * ```
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param entries DeleteMessageBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return [DeleteMessageBatchResponse] 인스턴스를 반환합니다.
 */
suspend inline fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")
    entries.requireNotEmpty("entries")

    return deleteMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toList()
    }
}
