package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Tag
import aws.sdk.kotlin.services.dynamodb.model.TagResourceRequest
import io.bluetape4k.support.requireNotBlank

@JvmName("tagResourceRequestOfTagList")
inline fun tagResourceRequestOf(
    resourceArn: String,
    tags: List<Tag>? = null,
    crossinline configurer: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return TagResourceRequest {
        this.resourceArn = resourceArn
        this.tags = tags

        configurer()
    }
}

@JvmName("tagResourceRequestOfTagArray")
inline fun tagResourceRequestOf(
    resourceArn: String,
    vararg tags: Tag,
    crossinline configurer: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return TagResourceRequest {
        this.resourceArn = resourceArn
        this.tags = tags.toList()

        configurer()
    }
}
