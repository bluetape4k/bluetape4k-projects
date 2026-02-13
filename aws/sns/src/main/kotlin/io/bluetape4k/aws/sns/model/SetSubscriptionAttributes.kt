package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest

inline fun setSubscriptionAttributesRequest(
    @BuilderInference builder: SetSubscriptionAttributesRequest.Builder.() -> Unit,
): SetSubscriptionAttributesRequest =
    SetSubscriptionAttributesRequest.builder().apply(builder).build()

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
