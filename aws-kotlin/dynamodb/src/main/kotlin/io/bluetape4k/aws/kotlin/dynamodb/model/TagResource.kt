package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Tag
import aws.sdk.kotlin.services.dynamodb.model.TagResourceRequest
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.support.requireNotBlank

@JvmName("tagResourceRequestOfTagList")
fun tagResourceRequestOf(
    resourceArn: String,
    tags: List<Tag>? = null,
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return TagResourceRequest {
        this.resourceArn = resourceArn
        this.tags = tags

        builder()
    }
}

@JvmName("tagResourceRequestOfTagArray")
fun tagResourceRequestOf(
    resourceArn: String,
    vararg tags: Tag,
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return TagResourceRequest {
        this.resourceArn = resourceArn
        this.tags = tags.toFastList()

        builder()
    }
}
