package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest

inline fun deleteTopicRequest(
    @BuilderInference builder: DeleteTopicRequest.Builder.() -> Unit,
): DeleteTopicRequest =
    DeleteTopicRequest.builder().apply(builder).build()

inline fun deleteTopicRequestOf(
    topicArn: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: DeleteTopicRequest.Builder.() -> Unit = {},
): DeleteTopicRequest {
    topicArn.requireNotBlank("topicArn")

    return deleteTopicRequest {
        topicArn(topicArn)
        overrideConfiguration?.let { overrideConfiguration(it) }
        builder()
    }
}
