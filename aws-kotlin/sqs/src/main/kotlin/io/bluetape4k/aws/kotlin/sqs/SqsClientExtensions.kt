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
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityBatchRequest
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityBatchResponse
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityRequest
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityResponse
import aws.sdk.kotlin.services.sqs.model.CreateQueueRequest
import aws.sdk.kotlin.services.sqs.model.CreateQueueResponse
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchResponse
import aws.sdk.kotlin.services.sqs.model.DeleteMessageRequest
import aws.sdk.kotlin.services.sqs.model.DeleteMessageResponse
import aws.sdk.kotlin.services.sqs.model.DeleteQueueRequest
import aws.sdk.kotlin.services.sqs.model.DeleteQueueResponse
import aws.sdk.kotlin.services.sqs.model.GetQueueUrlRequest
import aws.sdk.kotlin.services.sqs.model.ListQueuesRequest
import aws.sdk.kotlin.services.sqs.model.ListQueuesResponse
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageResponse
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequest
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
import io.bluetape4k.aws.kotlin.http.defaultCrtHttpEngineOf
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue

val log by lazy { KotlinLogging.logger { } }

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
 * @param endpoint Amazon SQS endpoint URL입니다.
 * @param region AWS region입니다.
 * @param credentialsProvider AWS credentials provider입니다.
 * @param httpClientEngine [HttpClientEngine] 엔진 (기본적으로 [aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine] 를 사용합니다.)
 * @param builder Amazon SQS client 설정 빌더입니다.
 * @return [SqsClient] 인스턴스를 반환합니다.
 */
fun sqsClientOf(
    endpoint: String,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClientEngine: HttpClientEngine = defaultCrtHttpEngineOf(),
    @BuilderInference builder: SqsClient.Config.Builder.() -> Unit = {},
): SqsClient {
    endpoint.requireNotBlank("endpoint")

    return SqsClient {
        this.endpointUrl = Url.parse(endpoint)
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }
        httpClient = httpClientEngine

        builder()
    }.apply {
        log.info { "Create SqlClient instance." }
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
suspend fun SqsClient.createQueue(
    queueName: String,
    @BuilderInference builder: CreateQueueRequest.Builder.() -> Unit = {},
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
suspend fun SqsClient.ensureQueue(
    queueName: String,
    @BuilderInference builder: CreateQueueRequest.Builder.() -> Unit = {},
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
suspend fun SqsClient.existsQueue(queueName: String): Boolean {
    return runCatching { getQueueUrl(queueName)?.isNotBlank() ?: false }.getOrDefault(false)
}

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
suspend fun SqsClient.listQueues(
    queueNamePrefix: String,
    nextToken: String? = null,
    maxResults: Int? = null,
    @BuilderInference builder: ListQueuesRequest.Builder.() -> Unit = {},
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
suspend fun SqsClient.getQueueUrl(
    queueName: String,
    @BuilderInference builder: GetQueueUrlRequest.Builder.() -> Unit = {},
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
suspend fun SqsClient.deleteQueue(
    queueUrl: String,
    @BuilderInference builder: DeleteQueueRequest.Builder.() -> Unit = {},
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
 * @param messageBody 전송할 메시지의 본문입니다.
 * @param delaySeconds 메시지를 보내기 전 대기할 시간(초)입니다. 기본값은 null입니다.
 * @param configurer SendMessageRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 * @return SendMessageResponse 인스턴스를 반환합니다.
 */
suspend fun SqsClient.send(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
    @BuilderInference builder: SendMessageRequest.Builder.() -> Unit = {},
): SendMessageResponse {
    queueUrl.requireNotBlank("queueUrl")

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
suspend fun SqsClient.sendBatch(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
    @BuilderInference builder: SendMessageBatchRequest.Builder.() -> Unit = {},
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")

    return sendMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
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
suspend fun SqsClient.sendBatch(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
    @BuilderInference builder: SendMessageBatchRequest.Builder.() -> Unit = {},
): SendMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")

    return sendMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
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
 * @param maxNumberOfMessages 한 번에 수신할 최대 메시지 수입니다. 기본값은 null입니다.
 * @param builder ReceiveMessageRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 */
suspend fun SqsClient.receive(
    queueUrl: String,
    maxNumberOfMessages: Int? = null,
    @BuilderInference builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageResponse {
    queueUrl.requireNotBlank("queueUrl")

    return receiveMessage {
        this.queueUrl = queueUrl
        maxNumberOfMessages?.let { this.maxNumberOfMessages = it }

        builder()
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
 *      // configurer
 * }
 * ```
 *
 * @param queueUrl 메시지의 Visibility를 변경할 Amazon SQS 큐의 URL입니다.
 * @param receiptHandle Visibility를 변경할 메시지와 연관된 영수증 핸들입니다.
 * @param visibilityTimeout 메시지의 새로운 VisibilityTimeout(초)입니다. 기본값은 null입니다.
 * @param builder ChangeMessageVisibilityRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 *
 * @return ChangeMessageVisibilityResponse 인스턴스를 반환합니다.
 */
suspend fun SqsClient.changeMessageVisibility(
    queueUrl: String,
    receiptHandle: String? = null,
    visibilityTimeout: Int? = null,
    @BuilderInference builder: ChangeMessageVisibilityRequest.Builder.() -> Unit = {},
): ChangeMessageVisibilityResponse {
    queueUrl.requireNotBlank("queueUrl")

    return changeMessageVisibility {
        this.queueUrl = queueUrl
        receiptHandle?.let { this.receiptHandle = it }
        visibilityTimeout?.let { this.visibilityTimeout = it }

        builder()
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
suspend fun SqsClient.changeVisibilityBatch(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
    @BuilderInference builder: ChangeMessageVisibilityBatchRequest.Builder.() -> Unit = {},
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")

    return changeMessageVisibilityBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
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
suspend fun SqsClient.changeVisibilityBatch(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
    @BuilderInference builder: ChangeMessageVisibilityBatchRequest.Builder.() -> Unit = {},
): ChangeMessageVisibilityBatchResponse {
    queueUrl.requireNotBlank("queueUrl")

    return changeMessageVisibilityBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
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
suspend fun SqsClient.deleteMessage(
    queueUrl: String,
    receiptHandle: String? = null,
    @BuilderInference builder: DeleteMessageRequest.Builder.() -> Unit = {},
): DeleteMessageResponse {
    queueUrl.requireNotBlank("queueUrl")

    return deleteMessage {
        this.queueUrl = queueUrl
        receiptHandle?.let { this.receiptHandle = it }

        builder()
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 메시지를 배치로 삭제합니다.
 *
 * ```
 * val response = sqsClient.deleteMessageBatch(
 *     "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue",
 *     DeleteMessageBatchRequestEntry {
 *          id = "id1"
 *          receiptHandle = "receiptHandle1"
 *     },
 *     DeleteMessageBatchRequestEntry {
 *          id = "id2"
 *          receiptHandle = "receiptHandle2"
 *     }
 * )
 * ```
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param entries DeleteMessageBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return [DeleteMessageBatchResponse] 인스턴스를 반환합니다.
 */
suspend fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
    @BuilderInference builder: DeleteMessageBatchRequest.Builder.() -> Unit = {},
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")

    return deleteMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
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
suspend fun SqsClient.deleteMessageBatch(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
    @BuilderInference builder: DeleteMessageBatchRequest.Builder.() -> Unit = {},
): DeleteMessageBatchResponse {
    queueUrl.requireNotBlank("queueUrl")

    return deleteMessageBatch {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
    }
}
