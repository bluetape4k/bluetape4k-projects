package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SubscribeRequest

inline fun SubscribeRequest(initializer: SubscribeRequest.Builder.() -> Unit): SubscribeRequest =
    SubscribeRequest.builder().apply(initializer).build()

fun subscribeRequestOf(
    topicArn: String,
    protocol: String,
    endpoint: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: SubscribeRequest.Builder.() -> Unit = {},
): SubscribeRequest = SubscribeRequest {
    topicArn(topicArn)
    protocol(protocol)
    endpoint(endpoint)
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
