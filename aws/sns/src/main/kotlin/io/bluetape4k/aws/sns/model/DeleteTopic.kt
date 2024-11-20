package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest

inline fun DeleteTopicRequest(
    initializer: DeleteTopicRequest.Builder.() -> Unit,
): DeleteTopicRequest =
    DeleteTopicRequest.builder().apply(initializer).build()

fun deleteTopicRequestOf(
    topicArn: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: DeleteTopicRequest.Builder.() -> Unit = {},
): DeleteTopicRequest = DeleteTopicRequest {
    topicArn(topicArn)
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
