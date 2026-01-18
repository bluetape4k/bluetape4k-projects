package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest

inline fun GetSubscriptionAttributesRequest(
    builder: GetSubscriptionAttributesRequest.Builder.() -> Unit,
): GetSubscriptionAttributesRequest =
    GetSubscriptionAttributesRequest.builder().apply(builder).build()

inline fun getSubscriptionAttributesRequestOf(
    subscriptionArn: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: GetSubscriptionAttributesRequest.Builder.() -> Unit = {},
): GetSubscriptionAttributesRequest = GetSubscriptionAttributesRequest {
    subscriptionArn?.run { subscriptionArn(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
