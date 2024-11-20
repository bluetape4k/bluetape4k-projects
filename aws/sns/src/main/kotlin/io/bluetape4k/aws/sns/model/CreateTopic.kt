package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.CreateTopicRequest

inline fun CreateTopicRequest(initializer: CreateTopicRequest.Builder.() -> Unit): CreateTopicRequest =
    CreateTopicRequest.builder().apply(initializer).build()

fun createTopicRequestOf(
    name: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: CreateTopicRequest.Builder.() -> Unit = {},
): CreateTopicRequest = CreateTopicRequest {
    name(name)
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
