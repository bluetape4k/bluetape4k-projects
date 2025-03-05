package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequest
import aws.sdk.kotlin.services.sqs.model.DeleteMessageBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.DeleteMessageRequest
import io.bluetape4k.support.requireNotBlank

/**
 * 제공된 queueUrl과 receiptHandle을 사용하여 DeleteMessageRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param receiptHandle 삭제할 메시지와 연관된 영수증 핸들입니다.
 * @param configurer DeleteMessageRequest.Builder를 사용하여 DeleteMessageRequest를 구성하는 람다입니다.
 */
inline fun deleteMessageRequestOf(
    queueUrl: String,
    receiptHandle: String? = null,
    crossinline configurer: DeleteMessageRequest.Builder.() -> Unit = {},
): DeleteMessageRequest {
    queueUrl.requireNotBlank("queueUrl")

    return DeleteMessageRequest {
        this.queueUrl = queueUrl
        receiptHandle?.let { this.receiptHandle = it }
        configurer()
    }
}

/**
 * 제공된 id와 receiptHandle을 사용하여 DeleteMessageBatchRequestEntry를 생성합니다.
 *
 * @param id 삭제할 메시지의 식별자입니다.
 * @param receiptHandle 삭제할 메시지와 연관된 영수증 핸들입니다.
 * @return DeleteMessageBatchRequestEntry 인스턴스를 반환합니다.
 */
inline fun deleteMessageBatchRequestEntryOf(
    id: String,
    receiptHandle: String? = null,
    crossinline configurer: DeleteMessageBatchRequestEntry.Builder.() -> Unit = {},
): DeleteMessageBatchRequestEntry {
    id.requireNotBlank("id")

    return DeleteMessageBatchRequestEntry {
        this.id = id
        receiptHandle?.let { this.receiptHandle = it }

        configurer()
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 DeleteMessageBatchRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param entries DeleteMessageBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return DeleteMessageBatchRequest 인스턴스를 반환합니다.
 */
inline fun deleteMessageBatchRequestOf(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
    crossinline configurer: DeleteMessageBatchRequest.Builder.() -> Unit = {},
): DeleteMessageBatchRequest {
    queueUrl.requireNotBlank("queueUrl")

    return DeleteMessageBatchRequest {
        this.queueUrl = queueUrl
        this.entries = entries.toList()

        configurer()
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 DeleteMessageBatchRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param entries DeleteMessageBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return DeleteMessageBatchRequest 인스턴스를 반환합니다.
 */
inline fun deleteMessageBatchRequestOf(
    queueUrl: String,
    vararg entries: DeleteMessageBatchRequestEntry,
    crossinline configurer: DeleteMessageBatchRequest.Builder.() -> Unit = {},
): DeleteMessageBatchRequest {
    queueUrl.requireNotBlank("queueUrl")

    return DeleteMessageBatchRequest {
        this.queueUrl = queueUrl
        this.entries = entries.toList()

        configurer()
    }
}
