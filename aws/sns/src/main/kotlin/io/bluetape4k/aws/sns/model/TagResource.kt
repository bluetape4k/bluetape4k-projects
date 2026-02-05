package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.Tag
import software.amazon.awssdk.services.sns.model.TagResourceRequest

inline fun TagResourceRequest(
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit,
): TagResourceRequest =
    TagResourceRequest.builder().apply(builder).build()

inline fun tagResourceRequestOf(
    resourceArn: String,
    tags: Collection<Tag>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest = TagResourceRequest {
    resourceArn(resourceArn)
    tags(tags)
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
