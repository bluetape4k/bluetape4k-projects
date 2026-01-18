package io.bluetape4k.aws.kotlin.sqs.model

import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber

/**
 * queueUrl, maxNumber, waitTimeSeconds, attributeNames를 사용하여 ReceiveMessageRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 수신할 Amazon SQS 큐의 URL입니다.
 * @param maxNumberOfMessages 한 번에 수신할 최대 메시지 수입니다. 기본값은 3입니다.
 * @param waitTimeSeconds 메시지가 없을 경우 대기할 시간(초)입니다. 기본값은 30초입니다.
 * @param visibilityTimeout 메시지를 처리하는 동안 숨겨진 시간(초)입니다. 기본값은 null입니다.
 * @param attributeNames 수신할 메시지의 속성 이름 컬렉션입니다. 기본값은 null입니다.
 * @param builder ReceiveMessageRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 * @return ReceiveMessageRequest 인스턴스를 반환합니다.
 */
inline fun receiveMessageRequestOf(
    queueUrl: String,
    maxNumberOfMessages: Int = 3,
    waitTimeSeconds: Int = 30,
    visibilityTimeout: Int? = null,
    attributeNames: Collection<String>? = null,
    crossinline builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageRequest {
    queueUrl.requireNotBlank("queueUrl")
    maxNumberOfMessages.requirePositiveNumber("maxNumberOfMessages")
    waitTimeSeconds.requirePositiveNumber("waitTimeSeconds")

    return ReceiveMessageRequest {
        this.queueUrl = queueUrl
        this.maxNumberOfMessages = maxNumberOfMessages
        this.waitTimeSeconds = waitTimeSeconds
        visibilityTimeout?.let { this.visibilityTimeout = it }
        attributeNames?.let { this.messageAttributeNames = it.toList() }

        builder()
    }
}
