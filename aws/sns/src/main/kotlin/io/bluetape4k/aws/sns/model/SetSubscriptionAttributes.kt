package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest

inline fun SetSubscriptionAttributesRequest(
    initializer: SetSubscriptionAttributesRequest.Builder.() -> Unit,
): SetSubscriptionAttributesRequest =
    SetSubscriptionAttributesRequest.builder().apply(initializer).build()

fun setSubscriptionAttributesRequestOf(
    subscriptionArn: String? = null,
    attributeName: String? = null,
    attributeValue: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: SetSubscriptionAttributesRequest.Builder.() -> Unit = {},
): SetSubscriptionAttributesRequest = SetSubscriptionAttributesRequest {
    subscriptionArn?.run { subscriptionArn(this) }
    attributeName?.run { attributeName(this) }
    attributeValue?.run { attributeValue(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
