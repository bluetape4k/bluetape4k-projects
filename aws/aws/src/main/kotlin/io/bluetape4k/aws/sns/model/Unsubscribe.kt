package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest

/**
 * DSL 블록으로 [UnsubscribeRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `subscriptionArn` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = unsubscribeRequest {
 *     subscriptionArn("arn:aws:sns:ap-northeast-2:123456:my-topic:sub-id")
 * }
 * ```
 */
inline fun unsubscribeRequest(
    builder: UnsubscribeRequest.Builder.() -> Unit,
): UnsubscribeRequest = UnsubscribeRequest.builder().apply(builder).build()

/**
 * 구독 ARN으로 [UnsubscribeRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [subscriptionArn]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = unsubscribeRequestOf("arn:aws:sns:ap-northeast-2:123456:my-topic:sub-id")
 * // req.subscriptionArn().isNotBlank() == true
 * ```
 */
inline fun unsubscribeRequestOf(
    subscriptionArn: String,
    builder: UnsubscribeRequest.Builder.() -> Unit = {},
): UnsubscribeRequest {
    subscriptionArn.requireNotBlank("subscriptionArn")

    return unsubscribeRequest {
        this.subscriptionArn(subscriptionArn)

        builder()
    }
}
