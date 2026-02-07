package io.bluetape4k.aws.kotlin.sns.model

import aws.sdk.kotlin.services.sns.model.GetTopicAttributesRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [topicArn]을 가지는 Topic 속성 조회 요청을 생성합니다.
 *
 * ```
 * val request = getTopicAttributesRequestOf("topicArn")
 * val attributes = client.getTopicAttributes(request)
 * ```
 *
 * @param topicArn Topic의 ARN
 * @param builder [GetTopicAttributesRequest.Builder]를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [GetTopicAttributesRequest] 인스턴스
 */
inline fun getTopicAttributesRequestOf(
    topicArn: String,
    @BuilderInference crossinline builder: GetTopicAttributesRequest.Builder.() -> Unit = {},
): GetTopicAttributesRequest {
    topicArn.requireNotBlank("topicArn")

    return GetTopicAttributesRequest {
        this.topicArn = topicArn
        builder()
    }
}
