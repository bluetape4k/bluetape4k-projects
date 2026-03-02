package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest

/**
 * DSL 블록으로 [SetSubscriptionAttributesRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `subscriptionArn`, `attributeName`, `attributeValue` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = setSubscriptionAttributesRequest {
 *     subscriptionArn("arn:aws:sns:ap-northeast-2:123456:my-topic:sub-id")
 *     attributeName("RawMessageDelivery")
 *     attributeValue("true")
 * }
 * ```
 */
inline fun setSubscriptionAttributesRequest(
    @BuilderInference builder: SetSubscriptionAttributesRequest.Builder.() -> Unit,
): SetSubscriptionAttributesRequest =
    SetSubscriptionAttributesRequest.builder().apply(builder).build()

/**
 * 구독 속성 설정을 위한 [SetSubscriptionAttributesRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - 모든 파라미터는 optional이며 null이면 설정되지 않는다.
 *
 * ```kotlin
 * val req = setSubscriptionAttributesRequestOf(
 *     subscriptionArn = "arn:aws:sns:ap-northeast-2:123456:my-topic:sub-id",
 *     attributeName = "RawMessageDelivery",
 *     attributeValue = "true"
 * )
 * ```
 */
inline fun setSubscriptionAttributesRequestOf(
    subscriptionArn: String? = null,
    attributeName: String? = null,
    attributeValue: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: SetSubscriptionAttributesRequest.Builder.() -> Unit = {},
): SetSubscriptionAttributesRequest = setSubscriptionAttributesRequest {
    subscriptionArn?.let { subscriptionArn(it) }
    attributeName?.let { attributeName(it) }
    attributeValue?.let { attributeValue(it) }
    overrideConfiguration?.let { overrideConfiguration(it) }

    builder()
}
