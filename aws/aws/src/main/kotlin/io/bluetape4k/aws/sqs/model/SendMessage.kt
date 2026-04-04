package io.bluetape4k.aws.sqs.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

/**
 * [SendMessageRequest] 를 생성합니다.
 *
 * @param builder [SendMessageRequest.Builder]를 이용하여 [SendMessageRequest]를 초기화하는 람다입니다.
 *
 * ```kotlin
 * val request = sendMessageRequest {
 *     queueUrl("https://sqs.ap-northeast-2.amazonaws.com/123/my-queue")
 *     messageBody("hello")
 * }
 * // request.messageBody() == "hello"
 * ```
 */
inline fun sendMessageRequest(
    builder: SendMessageRequest.Builder.() -> Unit,
): SendMessageRequest {
    return SendMessageRequest.builder().apply(builder).build()
}

/**
 * [queueUrl], [messageBody]로 [SendMessageRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = sendMessageRequestOf(
 *     queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/my-queue",
 *     messageBody = "hello",
 *     delaySeconds = 5
 * )
 * // request.delaySeconds() == 5
 * ```
 */
inline fun sendMessageRequestOf(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
    builder: SendMessageRequest.Builder.() -> Unit = {},
): SendMessageRequest {
    queueUrl.requireNotBlank("queueUrl")
    messageBody.requireNotBlank("messageBody")

    return sendMessageRequest {
        queueUrl(queueUrl)
        messageBody(messageBody)
        delaySeconds?.let { delaySeconds(it) }
        builder()
    }
}

/**
 * [SendMessageBatchRequestEntry] 를 생성합니다.
 *
 * @param builder [SendMessageBatchRequestEntry.Builder]를 이용하여 [SendMessageBatchRequestEntry]를 초기화하는 람다입니다.
 *
 * ```kotlin
 * val entry = sendMessageBatchRequestEntry {
 *     id("msg-1")
 *     messageBody("hello")
 * }
 * // entry.id() == "msg-1"
 * ```
 */
inline fun sendMessageBatchRequestEntry(
    builder: SendMessageBatchRequestEntry.Builder.() -> Unit,
): SendMessageBatchRequestEntry {
    return SendMessageBatchRequestEntry.builder().apply(builder).build()
}

/**
 * Build [SendMessageBatchRequestEntry]
 *
 * @param id                An identifier for the message in this batch.
 * @param messageGroupId    An identifier for the group of messages in this batch.
 * @param messageBody       The message to send.
 * @param delaySeconds      The length of time, in seconds, for which to delay a specific message.
 * @param builder       The lambda to initialize the builder.
 * @receiver            The builder to build the request.
 * @return            [SendMessageBatchRequestEntry] 인스턴스
 */
inline fun sendMessageBatchRequestEntryOf(
    id: String,
    messageGroupId: String,
    messageBody: String,
    delaySeconds: Int? = null,
    builder: SendMessageBatchRequestEntry.Builder.() -> Unit = {},
): SendMessageBatchRequestEntry {
    id.requireNotBlank("id")
    messageGroupId.requireNotBlank("messageGroupId")
    messageBody.requireNotBlank("messageBody")

    return sendMessageBatchRequestEntry {
        id(id)
        messageGroupId(messageGroupId)
        messageBody(messageBody)
        delaySeconds?.let { delaySeconds(it) }

        builder()
    }
}
