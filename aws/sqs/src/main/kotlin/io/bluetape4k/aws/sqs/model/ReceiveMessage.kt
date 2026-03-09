package io.bluetape4k.aws.sqs.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@PublishedApi
internal const val MIN_RECEIVE_MESSAGES = 1

@PublishedApi
internal const val MAX_RECEIVE_MESSAGES = 10

@PublishedApi
internal const val MIN_WAIT_TIME_SECONDS = 0

@PublishedApi
internal const val MAX_WAIT_TIME_SECONDS = 20

/**
 * [ReceiveMessageRequest]를 생성합니다.
 *
 * @param builder [ReceiveMessageRequest.Builder]를 이용하여 [ReceiveMessageRequest]를 초기화하는 람다입니다.
 */
inline fun receiveMessageRequest(
    @BuilderInference builder: ReceiveMessageRequest.Builder.() -> Unit,
): ReceiveMessageRequest {
    return ReceiveMessageRequest.builder().apply(builder).build()
}

/**
 * `queueUrl`, `maxNumber`, `waitTimeSeconds`, `attributeNames`를 사용하여 [ReceiveMessageRequest]를 생성합니다.
 *
 * @param queueUrl 메시지를 수신할 Amazon SQS 큐의 URL입니다.
 * @param maxNumber 한 번에 수신할 최대 메시지 수입니다. 기본값은 3입니다. (허용 범위: 1..10)
 * @param waitTimeSeconds 메시지가 없을 경우 대기할 시간(초)입니다. 기본값은 20초입니다. (허용 범위: 0..20)
 * @param attributeNames 수신할 메시지의 속성 이름 컬렉션입니다. 기본값은 null입니다.
 * @param builder ReceiveMessageRequest.Builder를 초기화하는 람다입니다. 기본값은 빈 람다입니다.
 * @return ReceiveMessageRequest 인스턴스를 반환합니다.
 */
inline fun receiveMessageRequestOf(
    queueUrl: String,
    maxNumber: Int = 3,
    waitTimeSeconds: Int = 20,
    attributeNames: Collection<String>? = null,
    @BuilderInference builder: ReceiveMessageRequest.Builder.() -> Unit = {},
): ReceiveMessageRequest {
    queueUrl.requireNotBlank("queueUrl")
    require(maxNumber in MIN_RECEIVE_MESSAGES..MAX_RECEIVE_MESSAGES) {
        "maxNumber must be in $MIN_RECEIVE_MESSAGES..$MAX_RECEIVE_MESSAGES, but was $maxNumber"
    }
    require(waitTimeSeconds in MIN_WAIT_TIME_SECONDS..MAX_WAIT_TIME_SECONDS) {
        "waitTimeSeconds must be in $MIN_WAIT_TIME_SECONDS..$MAX_WAIT_TIME_SECONDS, but was $waitTimeSeconds"
    }

    return receiveMessageRequest {
        queueUrl(queueUrl)
        maxNumberOfMessages(maxNumber)
        waitTimeSeconds(waitTimeSeconds)
        attributeNames?.let { messageAttributeNames(it) }

        builder()
    }
}
