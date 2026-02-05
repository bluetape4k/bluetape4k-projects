package io.bluetape4k.aws.kotlin.sns.model

import aws.sdk.kotlin.services.sns.model.ListTopicsRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [nextToken]을 가지는 Topic 목록 조회 요청을 생성합니다.
 *
 * ```
 * val request = listTopicsRequestOf("nextToken")
 * client.listTopics(request)
 * ```
 *
 * @param nextToken 다음 페이지를 조회하기 위한 토큰
 * @param builder [ListTopicsRequest.Builder]를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [ListTopicsRequest] 인스턴스
 */
fun listTopicsRequestOf(
    nextToken: String,
    @BuilderInference builder: ListTopicsRequest.Builder.() -> Unit = {},
): ListTopicsRequest {
    nextToken.requireNotBlank("nextToken")

    return ListTopicsRequest {
        this.nextToken = nextToken
        builder()
    }
}
