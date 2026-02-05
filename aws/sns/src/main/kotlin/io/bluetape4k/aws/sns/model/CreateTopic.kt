package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.CreateTopicRequest

inline fun CreateTopicRequest(
    @BuilderInference builder: CreateTopicRequest.Builder.() -> Unit,
): CreateTopicRequest =
    CreateTopicRequest.builder().apply(builder).build()

inline fun createTopicRequestOf(
    name: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: CreateTopicRequest.Builder.() -> Unit = {},
): CreateTopicRequest = CreateTopicRequest {
    name(name)
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
