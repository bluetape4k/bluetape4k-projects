package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest

inline fun SetSubscriptionAttributesRequest(
    @BuilderInference builder: SetSubscriptionAttributesRequest.Builder.() -> Unit,
): SetSubscriptionAttributesRequest =
    SetSubscriptionAttributesRequest.builder().apply(builder).build()

inline fun setSubscriptionAttributesRequestOf(
    subscriptionArn: String? = null,
    attributeName: String? = null,
    attributeValue: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: SetSubscriptionAttributesRequest.Builder.() -> Unit = {},
): SetSubscriptionAttributesRequest = SetSubscriptionAttributesRequest {
    subscriptionArn?.run { subscriptionArn(this) }
    attributeName?.run { attributeName(this) }
    attributeValue?.run { attributeValue(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
