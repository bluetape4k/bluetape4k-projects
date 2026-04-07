package io.bluetape4k.aws.sqs.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest

/**
 * 제공된 초기화자를 사용하여 DeleteMessageRequest를 구성합니다.
 *
 * @param builder DeleteMessageRequest.Builder를 초기화하는 람다입니다.
 * @return DeleteMessageRequest 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val request = deleteMessageRequest {
 *     queueUrl("https://sqs.ap-northeast-2.amazonaws.com/123/my-queue")
 *     receiptHandle("handle-xyz")
 * }
 * // request.receiptHandle() == "handle-xyz"
 * ```
 */
inline fun deleteMessageRequest(
    builder: DeleteMessageRequest.Builder.() -> Unit,
): DeleteMessageRequest {
    return DeleteMessageRequest.builder().apply(builder).build()
}

/**
 * 제공된 queueUrl과 receiptHandle을 사용하여 DeleteMessageRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param receiptHandle 삭제할 메시지와 연관된 영수증 핸들입니다.
 * @return DeleteMessageRequest 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val request = deleteMessageRequestOf(
 *     queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/my-queue",
 *     receiptHandle = "handle-xyz"
 * )
 * // request.queueUrl().contains("my-queue") == true
 * ```
 */
fun deleteMessageRequestOf(
    queueUrl: String,
    receiptHandle: String,
): DeleteMessageRequest {
    queueUrl.requireNotBlank("queueUrl")
    receiptHandle.requireNotBlank("receiptHandle")

    return deleteMessageRequest {
        queueUrl(queueUrl)
        receiptHandle(receiptHandle)
    }
}

/**
 * 제공된 초기화자를 사용하여 DeleteMessageBatchRequest를 구성합니다.
 *
 * @param builder DeleteMessageBatchRequest.Builder를 초기화하는 람다입니다.
 * @return DeleteMessageBatchRequest 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val request = deleteMessageBatchRequest {
 *     queueUrl("https://sqs.ap-northeast-2.amazonaws.com/123/my-queue")
 * }
 * // request.queueUrl().contains("my-queue") == true
 * ```
 */
inline fun deleteMessageBatchRequest(
    builder: DeleteMessageBatchRequest.Builder.() -> Unit,
): DeleteMessageBatchRequest {
    return DeleteMessageBatchRequest.builder().apply(builder).build()
}

/**
 * 제공된 queueUrl과 entries를 사용하여 DeleteMessageBatchRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 삭제할 Amazon SQS 큐의 URL입니다.
 * @param entries DeleteMessageBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return DeleteMessageBatchRequest 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val entry = deleteMessageBatchRequestEntryOf("msg-1", "handle-xyz")
 * val request = deleteMessageBatchRequestOf("https://sqs.ap-northeast-2.amazonaws.com/123/my-queue", listOf(entry))
 * // request.entries().size == 1
 * ```
 */
fun deleteMessageBatchRequestOf(
    queueUrl: String,
    entries: Collection<DeleteMessageBatchRequestEntry>,
): DeleteMessageBatchRequest {
    queueUrl.requireNotBlank("queueUrl")

    return deleteMessageBatchRequest {
        queueUrl(queueUrl)
        entries(entries)
    }
}

/**
 * 제공된 초기화자를 사용하여 DeleteMessageBatchRequestEntry를 구성합니다.
 *
 * @param builder DeleteMessageBatchRequestEntry.Builder를 초기화하는 람다입니다.
 * @return DeleteMessageBatchRequestEntry 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val entry = deleteMessageBatchRequestEntry {
 *     id("msg-1")
 *     receiptHandle("handle-xyz")
 * }
 * // entry.id() == "msg-1"
 * ```
 */
inline fun deleteMessageBatchRequestEntry(
    builder: DeleteMessageBatchRequestEntry.Builder.() -> Unit,
): DeleteMessageBatchRequestEntry {
    return DeleteMessageBatchRequestEntry.builder().apply(builder).build()
}

/**
 * 제공된 id와 receiptHandle을 사용하여 DeleteMessageBatchRequestEntry를 생성합니다.
 *
 * @param id 삭제할 메시지의 식별자입니다.
 * @param receiptHandle 삭제할 메시지와 연관된 영수증 핸들입니다.
 * @return DeleteMessageBatchRequestEntry 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val entry = deleteMessageBatchRequestEntryOf("msg-1", "handle-xyz")
 * // entry.receiptHandle() == "handle-xyz"
 * ```
 */
fun deleteMessageBatchRequestEntryOf(
    id: String,
    receiptHandle: String,
): DeleteMessageBatchRequestEntry {
    id.requireNotBlank("id")
    receiptHandle.requireNotBlank("receiptHandle")

    return deleteMessageBatchRequestEntry {
        id(id)
        receiptHandle(receiptHandle)
    }
}
