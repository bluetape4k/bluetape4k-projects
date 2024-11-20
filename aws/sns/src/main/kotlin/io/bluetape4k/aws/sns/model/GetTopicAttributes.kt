package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest

inline fun GetTopicAttributesRequest(
    initializer: GetTopicAttributesRequest.Builder.() -> Unit,
): GetTopicAttributesRequest =
    GetTopicAttributesRequest.builder().apply(initializer).build()

fun getTopicAttributesRequestOf(
    topicArn: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: GetTopicAttributesRequest.Builder.() -> Unit = {},
): GetTopicAttributesRequest = GetTopicAttributesRequest {
    topicArn?.run { topicArn(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
