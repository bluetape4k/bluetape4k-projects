package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.Tag
import software.amazon.awssdk.services.sns.model.TagResourceRequest

inline fun tagResourceRequest(
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit,
): TagResourceRequest =
    TagResourceRequest.builder().apply(builder).build()

inline fun tagResourceRequestOf(
    resourceArn: String,
    tags: Collection<Tag>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return tagResourceRequest {
        resourceArn(resourceArn)
        tags(tags)
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
}
