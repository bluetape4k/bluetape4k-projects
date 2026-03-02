package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest

/**
 * DSL 블록으로 [GetSubscriptionAttributesRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `subscriptionArn` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = getSubscriptionAttributesRequest {
 *     subscriptionArn("arn:aws:sns:ap-northeast-2:123456:my-topic:sub-id")
 * }
 * ```
 */
inline fun getSubscriptionAttributesRequest(
    @BuilderInference builder: GetSubscriptionAttributesRequest.Builder.() -> Unit,
): GetSubscriptionAttributesRequest =
    GetSubscriptionAttributesRequest.builder().apply(builder).build()

/**
 * 구독 ARN으로 [GetSubscriptionAttributesRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [subscriptionArn]이 non-null이면서 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = getSubscriptionAttributesRequestOf("arn:aws:sns:ap-northeast-2:123456:my-topic:sub-id")
 * // req.subscriptionArn().isNotBlank() == true
 * ```
 */
inline fun getSubscriptionAttributesRequestOf(
    subscriptionArn: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: GetSubscriptionAttributesRequest.Builder.() -> Unit = {},
): GetSubscriptionAttributesRequest =
    getSubscriptionAttributesRequest {
        subscriptionArn?.let {
            it.requireNotBlank("subscriptionArn")
            subscriptionArn(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
