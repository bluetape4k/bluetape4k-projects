package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

inline fun publishRequest(
    @BuilderInference builder: PublishRequest.Builder.() -> Unit,
): PublishRequest =
    PublishRequest.builder().apply(builder).build()

inline fun publishRequestOf(
    topicArn: String,
    message: String,
    snsAttributes: Map<String, MessageAttributeValue>? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: PublishRequest.Builder.() -> Unit = {},
): PublishRequest {
    topicArn.requireNotBlank("topicArn")
    message.requireNotBlank("message")

    return publishRequest {
        topicArn(topicArn)
        message(message)
        snsAttributes?.let { messageAttributes(it) }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
}
