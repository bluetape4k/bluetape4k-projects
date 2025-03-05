package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import io.bluetape4k.support.requireNotBlank

/**
 * 제공된 queueUrl과 messageBody를 사용하여 SendMessageRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 보낼 Amazon SQS 큐의 URL입니다.
 * @param messageBody 전송할 메시지의 본문입니다.
 * @param delaySeconds 메시지를 보내기 전 대기할 시간(초)입니다. 기본값은 null입니다.
 * @param configurer SendMessageRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 */
inline fun sendMessageRequestOf(
    queueUrl: String,
    messageBody: String,
    delaySeconds: Int? = null,
    crossinline configurer: SendMessageRequest.Builder.() -> Unit = {},
): SendMessageRequest {
    queueUrl.requireNotBlank("queueUrl")
    messageBody.requireNotBlank("messageBody")

    return SendMessageRequest {
        this.queueUrl = queueUrl
        this.messageBody = messageBody
        delaySeconds?.let { this.delaySeconds = it }

        configurer()
    }
}

/**
 * 제공된 id, messageBody, messageGroupId를 사용하여 SendMessageBatchRequestEntry를 생성합니다.
 *
 * @param id 메시지의 식별자입니다.
 * @param messageBody 전송할 메시지의 본문입니다.
 * @param messageGroupId 메시지 그룹의 식별자입니다.
 * @param delaySeconds 메시지를 보내기 전 대기할 시간(초)입니다. 기본값은 null입니다.
 * @param configurer SendMessageBatchRequestEntry.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 *
 * @return SendMessageBatchRequestEntry 인스턴스를 반환합니다.
 */
inline fun sendMessageBatchRequestEntryOf(
    id: String,
    messageBody: String,
    messageGroupId: String? = null,
    delaySeconds: Int? = null,
    crossinline configurer: SendMessageBatchRequestEntry.Builder.() -> Unit = {},
): SendMessageBatchRequestEntry {
    id.requireNotBlank("id")
    messageBody.requireNotBlank("messageBody")

    return SendMessageBatchRequestEntry {
        this.id = id
        this.messageBody = messageBody
        messageGroupId?.let { this.messageGroupId = it }
        delaySeconds?.let { this.delaySeconds = it }

        configurer()
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 SendMessageBatchRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 보낼 Amazon SQS 큐의 URL입니다.
 * @param entries SendMessageBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return SendMessageBatchRequest 인스턴스를 반환합니다.
 */
@JvmName("sendMessageBatchRequestOfCollection")
inline fun sendMessageBatchRequestOf(
    queueUrl: String,
    entries: Collection<SendMessageBatchRequestEntry>,
    crossinline configurer: SendMessageBatchRequest.Builder.() -> Unit = {},
): SendMessageBatchRequest {
    queueUrl.requireNotBlank("queueUrl")

    return SendMessageBatchRequest {
        this.queueUrl = queueUrl
        this.entries = entries.toList()

        configurer()
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 SendMessageBatchRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 보낼 Amazon SQS 큐의 URL입니다.
 * @param entries SendMessageBatchRequestEntry 인스턴스의 배열입니다.
 * @return SendMessageBatchRequest 인스턴스를 반환합니다.
 */
@JvmName("sendMessageBatchRequestOfArray")
inline fun sendMessageBatchRequestOf(
    queueUrl: String,
    vararg entries: SendMessageBatchRequestEntry,
    crossinline configurer: SendMessageBatchRequest.Builder.() -> Unit = {},
): SendMessageBatchRequest {
    queueUrl.requireNotBlank("queueUrl")

    return SendMessageBatchRequest {
        this.queueUrl = queueUrl
        this.entries = entries.toList()

        configurer()
    }
}
