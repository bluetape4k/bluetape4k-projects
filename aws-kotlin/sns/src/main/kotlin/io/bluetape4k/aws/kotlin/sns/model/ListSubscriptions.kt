package io.bluetape4k.aws.kotlin.sns.model

import aws.sdk.kotlin.services.sns.model.ListSubscriptionsRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [nextToken]을 가지는 Subscription 목록 조회 요청을 생성합니다.
 *
 * ```
 * val request = listSubscriptionsRequestOf("nextToken")
 * val subscriptions = client.listSubscriptions(request)
 * ```
 *
 * @param nextToken 다음 페이지를 조회하기 위한 토큰
 * @param configurer [ListSubscriptionsRequest.Builder]를 통해 추가적인 설정을 할 수 있는 람다 함수
 * @return [ListSubscriptionsRequest] 인스턴스
 */
fun listSubscriptinosRequestOf(
    nextToken: String,
    configurer: ListSubscriptionsRequest.Builder.() -> Unit = {},
): ListSubscriptionsRequest {
    nextToken.requireNotBlank("nextToken")

    return ListSubscriptionsRequest {
        this.nextToken = nextToken
        configurer()
    }
}
