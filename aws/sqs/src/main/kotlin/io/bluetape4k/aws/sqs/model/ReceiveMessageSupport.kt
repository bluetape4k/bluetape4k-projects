package io.bluetape4k.aws.sqs.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

/**
 * [ReceiveMessageRequest]를 생성합니다.
 *
 * @param initializer [ReceiveMessageRequest.Builder]를 이용하여 [ReceiveMessageRequest]를 초기화하는 람다입니다.
 */
inline fun receiveMessageRequest(
    initializer: ReceiveMessageRequest.Builder.() -> Unit,
): ReceiveMessageRequest {
    return ReceiveMessageRequest.builder().apply(initializer).build()
}

/**
 * queueUrl, maxNumber, waitTimeSeconds, attributeNames를 사용하여 ReceiveMessageRequest를 생성합니다.
 *
 * @param queueUrl 메시지를 수신할 Amazon SQS 큐의 URL입니다.
 * @param maxNumber 한 번에 수신할 최대 메시지 수입니다. 기본값은 3입니다.
 * @param waitTimeSeconds 메시지가 없을 경우 대기할 시간(초)입니다. 기본값은 30초입니다.
 * @param attributeNames 수신할 메시지의 속성 이름 컬렉션입니다. 기본값은 null입니다.
 * @param initializer ReceiveMessageRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 * @return ReceiveMessageRequest 인스턴스를 반환합니다.
 */
fun receiveMessageRequestOf(
    queueUrl: String,
    maxNumber: Int = 3,
    waitTimeSeconds: Int = 30,
    attributeNames: Collection<String>? = null,
    initializer: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageRequest {
    queueUrl.requireNotBlank("queueUrl")

    return receiveMessageRequest {
        queueUrl(queueUrl)
        maxNumberOfMessages(maxNumber)
        waitTimeSeconds(waitTimeSeconds)
        attributeNames?.run { messageAttributeNames(this) }
        initializer()
    }
}
