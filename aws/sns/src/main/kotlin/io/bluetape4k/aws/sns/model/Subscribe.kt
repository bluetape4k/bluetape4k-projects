package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SubscribeRequest

inline fun SubscribeRequest(
    @BuilderInference builder: SubscribeRequest.Builder.() -> Unit,
): SubscribeRequest =
    SubscribeRequest.builder().apply(builder).build()

inline fun subscribeRequestOf(
    topicArn: String,
    protocol: String,
    endpoint: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: SubscribeRequest.Builder.() -> Unit = {},
): SubscribeRequest = SubscribeRequest {
    topicArn(topicArn)
    protocol(protocol)
    endpoint(endpoint)
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
