package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest

inline fun DeleteTopicRequest(
    builder: DeleteTopicRequest.Builder.() -> Unit,
): DeleteTopicRequest =
    DeleteTopicRequest.builder().apply(builder).build()

inline fun deleteTopicRequestOf(
    topicArn: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: DeleteTopicRequest.Builder.() -> Unit = {},
): DeleteTopicRequest = DeleteTopicRequest {
    topicArn(topicArn)
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
