package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest

inline fun getSubscriptionAttributesRequest(
    @BuilderInference builder: GetSubscriptionAttributesRequest.Builder.() -> Unit,
): GetSubscriptionAttributesRequest =
    GetSubscriptionAttributesRequest.builder().apply(builder).build()

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
