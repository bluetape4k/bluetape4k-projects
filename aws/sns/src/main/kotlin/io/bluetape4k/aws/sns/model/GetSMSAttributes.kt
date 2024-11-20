package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest

inline fun GetSubscriptionAttributesRequest(
    initializer: GetSubscriptionAttributesRequest.Builder.() -> Unit,
): GetSubscriptionAttributesRequest =
    GetSubscriptionAttributesRequest.builder().apply(initializer).build()

fun getSubscriptionAttributesRequestOf(
    subscriptionArn: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: GetSubscriptionAttributesRequest.Builder.() -> Unit = {},
): GetSubscriptionAttributesRequest = GetSubscriptionAttributesRequest {
    subscriptionArn?.run { subscriptionArn(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
