package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

inline fun PublishRequest(initializer: PublishRequest.Builder.() -> Unit): PublishRequest =
    PublishRequest.builder().apply(initializer).build()

fun publishRequestOf(
    topicArn: String,
    message: String,
    snsAttributes: Map<String, MessageAttributeValue>? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: PublishRequest.Builder.() -> Unit = {},
): PublishRequest = PublishRequest {
    topicArn(topicArn)
    message(message)
    snsAttributes?.run { messageAttributes(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
