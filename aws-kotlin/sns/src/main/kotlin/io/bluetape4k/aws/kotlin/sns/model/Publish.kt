package io.bluetape4k.aws.kotlin.sns.model

import aws.sdk.kotlin.services.sns.model.MessageAttributeValue
import aws.sdk.kotlin.services.sns.model.PublishBatchRequestEntry
import aws.sdk.kotlin.services.sns.model.PublishRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [topicArn] Topic에 [message]를 발행 요청하는 [PublishRequest]를 생성합니다.
 *
 * ```
 * val request = publishRequestOf(
 *    topicArn = "arn:aws:sns:ap-northeast-2:123456789012:MyTopic",
 *    message = "Hello, SNS!"
 * )
 * client.publish(request)
 * ```
 *
 * @param topicArn 발행할 Topic의 ARN
 * @param phoneNumber 발행할 전화번호
 * @param message 발행할 메시지
 * @param subject 메시지 제목
 * @param messageAttributes 메시지 속성
 * @param messageDeduplicationId 메시지 중복 제거 ID
 * @param messageGroupId 메시지 그룹 ID
 * @param builder [PublishRequest.Builder]를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [PublishRequest] 인스턴스
 */
inline fun publishRequestOf(
    topicArn: String,
    phoneNumber: String,
    message: String,
    subject: String? = null,
    messageAttributes: Map<String, MessageAttributeValue>? = null,
    messageDeduplicationId: String? = null,
    messageGroupId: String? = null,
    @BuilderInference crossinline builder: PublishRequest.Builder.() -> Unit = {},
): PublishRequest {
    topicArn.requireNotBlank("topicArn")
    phoneNumber.requireNotBlank("phoneNumber")
    message.requireNotBlank("message")

    return PublishRequest {
        this.topicArn = topicArn
        this.phoneNumber = phoneNumber
        this.message = message
        subject?.let { this.subject = it }
        messageAttributes?.let { this.messageAttributes = it }
        messageDeduplicationId?.let { this.messageDeduplicationId = it }
        messageGroupId?.let { this.messageGroupId = it }

        builder()
    }
}

/**
 * SNS 배치 발행 요청의 개별 항목 [PublishBatchRequestEntry]를 생성합니다.
 *
 * ```
 * val entry = publishBatchRequestEntryOf(
 *     id = "msg-001",
 *     message = "Hello, SNS!",
 *     messageGroupId = "group1"
 * )
 * ```
 *
 * @param id 배치 항목의 고유 식별자
 * @param message 발행할 메시지 내용
 * @param messageAttributes 메시지 속성 맵
 * @param messageDeduplicationId 메시지 중복 제거 ID (FIFO 토픽 전용)
 * @param messageGroupId 메시지 그룹 ID (FIFO 토픽 전용)
 * @param builder [PublishBatchRequestEntry.Builder]를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [PublishBatchRequestEntry] 인스턴스
 */
inline fun publishBatchRequestEntryOf(
    id: String,
    message: String,
    messageAttributes: Map<String, MessageAttributeValue>? = null,
    messageDeduplicationId: String? = null,
    messageGroupId: String? = null,
    @BuilderInference crossinline builder: PublishBatchRequestEntry.Builder.() -> Unit = {},
): PublishBatchRequestEntry {
    id.requireNotBlank("id")
    message.requireNotBlank("message")

    return PublishBatchRequestEntry {
        this.id = id
        this.message = message
        messageAttributes?.let { this.messageAttributes = it }
        messageDeduplicationId?.let { this.messageDeduplicationId = it }
        messageGroupId?.let { this.messageGroupId = it }

        builder()
    }
}
