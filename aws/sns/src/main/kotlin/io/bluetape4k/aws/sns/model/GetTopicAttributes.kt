package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest

inline fun GetTopicAttributesRequest(
    builder: GetTopicAttributesRequest.Builder.() -> Unit,
): GetTopicAttributesRequest =
    GetTopicAttributesRequest.builder().apply(builder).build()

inline fun getTopicAttributesRequestOf(
    topicArn: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: GetTopicAttributesRequest.Builder.() -> Unit = {},
): GetTopicAttributesRequest = GetTopicAttributesRequest {
    topicArn?.run { topicArn(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
