package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.Tag
import software.amazon.awssdk.services.sns.model.TagResourceRequest

inline fun TagResourceRequest(
    initializer: TagResourceRequest.Builder.() -> Unit,
): TagResourceRequest =
    TagResourceRequest.builder().apply(initializer).build()

fun tagResourceRequestOf(
    resourceArn: String,
    tags: Collection<Tag>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest = TagResourceRequest {
    resourceArn(resourceArn)
    tags(tags)
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
