package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest

/**
 * DSL 블록으로 [ConfirmSubscriptionRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `topicArn`, `token` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = confirmSubscriptionRequest {
 *     topicArn("arn:aws:sns:ap-northeast-2:123456:my-topic")
 *     token("abc123token")
 * }
 * ```
 */
inline fun confirmSubscriptionRequest(
    @BuilderInference builder: ConfirmSubscriptionRequest.Builder.() -> Unit,
): ConfirmSubscriptionRequest =
    ConfirmSubscriptionRequest.builder().apply(builder).build()

/**
 * 토픽 ARN과 확인 토큰으로 [ConfirmSubscriptionRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [topicArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [token]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = confirmSubscriptionRequestOf(
 *     topicArn = "arn:aws:sns:ap-northeast-2:123456:my-topic",
 *     token = "abc123token"
 * )
 * // req.topicArn().isNotBlank() == true
 * ```
 */
inline fun confirmSubscriptionRequestOf(
    topicArn: String,
    token: String,
    @BuilderInference builder: ConfirmSubscriptionRequest.Builder.() -> Unit = {},
): ConfirmSubscriptionRequest {
    topicArn.requireNotBlank("topicArn")
    token.requireNotBlank("token")

    return confirmSubscriptionRequest {
        topicArn(topicArn)
        token(token)
        builder()
    }
}
