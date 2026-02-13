package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest

inline fun getTopicAttributesRequest(
    @BuilderInference builder: GetTopicAttributesRequest.Builder.() -> Unit,
): GetTopicAttributesRequest =
    GetTopicAttributesRequest.builder().apply(builder).build()

inline fun getTopicAttributesRequestOf(
    topicArn: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: GetTopicAttributesRequest.Builder.() -> Unit = {},
): GetTopicAttributesRequest =
    getTopicAttributesRequest {
        topicArn?.let {
            topicArn.requireNotBlank("topicArn")
            topicArn(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
