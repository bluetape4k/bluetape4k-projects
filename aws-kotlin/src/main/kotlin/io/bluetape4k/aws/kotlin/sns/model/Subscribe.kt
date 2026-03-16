package io.bluetape4k.aws.kotlin.sns.model

import aws.sdk.kotlin.services.sns.model.SubscribeRequest
import aws.sdk.kotlin.services.sns.model.UnsubscribeRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [topicArn]의 Topic을 구독 요청하는 [SubscribeRequest]를 생성합니다.
 *
 * ```
 * val request = subscribeRequestOf(
 *    topicArn = "arn:aws:sns:ap-northeast-2:123456789012:MyTopic",
 *    endpoint = "+821012345678",
 *    protocol = "sms"
 * )
 * client.subscribe(request)
 * ```
 *
 * @param topicArn 구독할 Topic의 ARN
 * @param endpoint 구독할 Endpoint
 * @param protocol 구독할 Endpoint의 프로토콜
 * @param builder [SubscribeRequest.Builder]를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [SubscribeRequest] 인스턴스
 */
inline fun subscribeRequestOf(
    topicArn: String,
    endpoint: String,
    protocol: String = "sms",
    @BuilderInference crossinline builder: SubscribeRequest.Builder.() -> Unit = {},
): SubscribeRequest {
    topicArn.requireNotBlank("topicArn")
    protocol.requireNotBlank("protocol")
    endpoint.requireNotBlank("endpoint")

    return SubscribeRequest {
        this.topicArn = topicArn
        this.protocol = protocol
        this.endpoint = endpoint

        builder()
    }
}

/**
 * [subscriptionArn]의 구독 취소 요청 [UnsubscribeRequest]를 생성합니다.
 *
 * ```
 * val request = unsubscribeRequestOf("arn:aws:sns:ap-northeast-2:123456789012:MyTopic:12345678-1234-1234-1234-123456789012")
 * client.unsubscribe(request)
 * ```
 *
 * @param subscriptionArn 취소할 구독의 ARN
 * @param builder [UnsubscribeRequest.Builder]를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [UnsubscribeRequest] 인스턴스
 */
inline fun unsubscribeRequestOf(
    subscriptionArn: String,
    @BuilderInference crossinline builder: UnsubscribeRequest.Builder.() -> Unit = {},
): UnsubscribeRequest {
    subscriptionArn.requireNotBlank("subscriptionArn")

    return UnsubscribeRequest {
        this.subscriptionArn = subscriptionArn
        builder()
    }
}
