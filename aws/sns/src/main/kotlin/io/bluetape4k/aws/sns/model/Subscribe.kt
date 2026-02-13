package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SubscribeRequest

inline fun subscribeRequest(
    @BuilderInference builder: SubscribeRequest.Builder.() -> Unit,
): SubscribeRequest =
    SubscribeRequest.builder().apply(builder).build()

inline fun subscribeRequestOf(
    topicArn: String,
    protocol: String,
    endpoint: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: SubscribeRequest.Builder.() -> Unit = {},
): SubscribeRequest {
    topicArn.requireNotBlank("topicArn")
    protocol.requireNotBlank("protocol")
    endpoint.requireNotBlank("endpoint")

    return subscribeRequest {
        topicArn(topicArn)
        protocol(protocol)
        endpoint(endpoint)
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
}
