package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityBatchRequest
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry
import aws.sdk.kotlin.services.sqs.model.ChangeMessageVisibilityRequest
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.support.requireNotBlank

/**
 * 제공된 queueUrl과 receiptHandle을 사용하여 [ChangeMessageVisibilityRequest] 를 생성합니다.
 *
 * @param queueUrl 메시지의 Visibility를 변경할 Amazon SQS 큐의 URL입니다.
 * @param receiptHandle Visibility를 변경할 메시지와 연관된 영수증 핸들입니다.
 * @param visibilityTimeout 메시지의 새로운 VisibilityTimeout(초)입니다. 기본값은 null입니다.
 * @return [ChangeMessageVisibilityRequest] 인스턴스를 반환합니다.
 */
inline fun changeMessageVisibilityRequestOf(
    queueUrl: String,
    receiptHandle: String,
    visibilityTimeout: Int? = null,
    crossinline builder: ChangeMessageVisibilityRequest.Builder.() -> Unit = {},
): ChangeMessageVisibilityRequest {
    queueUrl.requireNotBlank("queueUrl")
    receiptHandle.requireNotBlank("receiptHandle")

    return ChangeMessageVisibilityRequest {
        this.queueUrl = queueUrl
        this.receiptHandle = receiptHandle
        this.visibilityTimeout = visibilityTimeout

        builder()
    }
}


/**
 * 제공된 id와 receiptHandle을 사용하여 [ChangeMessageVisibilityBatchRequestEntry] 를 생성합니다.
 *
 * @param id 메시지의 식별자입니다.
 * @param receiptHandle Visibility를 변경할 메시지와 연관된 영수증 핸들입니다.
 * @param visibilityTimeout 메시지의 새로운 VisibilityTimeout(초)입니다. 기본값은 null입니다.
 */
inline fun changeMessageVisibilityBatchRequestEntryOf(
    id: String,
    receiptHandle: String,
    visibilityTimeout: Int? = null,
    crossinline builder: ChangeMessageVisibilityBatchRequestEntry.Builder.() -> Unit = {},
): ChangeMessageVisibilityBatchRequestEntry {
    id.requireNotBlank("id")
    receiptHandle.requireNotBlank("receiptHandle")

    return ChangeMessageVisibilityBatchRequestEntry {
        this.id = id
        this.receiptHandle = receiptHandle
        this.visibilityTimeout = visibilityTimeout

        builder()
    }
}

/**
 * 제공된 queueUrl과 entries를 사용하여 [ChangeMessageVisibilityBatchRequest] 를 생성합니다.
 *
 * @param queueUrl 메시지의 Visibility를 변경할 Amazon SQS 큐의 URL입니다.
 * @param entries ChangeMessageVisibilityBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return [ChangeMessageVisibilityBatchRequest] 인스턴스를 반환합니다.
 */
@JvmName("changeMessageVisibilityBatchRequestOfCollection")
inline fun changeMessageVisibilityBatchRequestOf(
    queueUrl: String,
    entries: Collection<ChangeMessageVisibilityBatchRequestEntry>,
    crossinline builder: ChangeMessageVisibilityBatchRequest.Builder.() -> Unit = {},
): ChangeMessageVisibilityBatchRequest {
    queueUrl.requireNotBlank("queueUrl")

    return ChangeMessageVisibilityBatchRequest {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
    }
}


/**
 * 제공된 queueUrl과 entries를 사용하여 [ChangeMessageVisibilityBatchRequest] 를 생성합니다.
 *
 * @param queueUrl 메시지의 Visibility를 변경할 Amazon SQS 큐의 URL입니다.
 * @param entries ChangeMessageVisibilityBatchRequestEntry 인스턴스의 컬렉션입니다.
 * @return [ChangeMessageVisibilityBatchRequest] 인스턴스를 반환합니다.
 */
@JvmName("changeMessageVisibilityBatchRequestOfVararg")
inline fun changeMessageVisibilityBatchRequestOf(
    queueUrl: String,
    vararg entries: ChangeMessageVisibilityBatchRequestEntry,
    crossinline builder: ChangeMessageVisibilityBatchRequest.Builder.() -> Unit = {},
): ChangeMessageVisibilityBatchRequest {
    queueUrl.requireNotBlank("queueUrl")

    return ChangeMessageVisibilityBatchRequest {
        this.queueUrl = queueUrl
        this.entries = entries.toFastList()

        builder()
    }
}
