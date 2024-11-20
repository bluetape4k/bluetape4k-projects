package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Tag
import aws.sdk.kotlin.services.dynamodb.model.TagResourceRequest
import io.bluetape4k.support.requireNotBlank

fun tagResourceRequestOf(
    resourceArn: String,
    tags: List<Tag>? = null,
    configurer: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return TagResourceRequest {
        this.resourceArn = resourceArn
        this.tags = tags
        configurer()
    }
}

fun tagResourceRequestOf(resourceArn: String, vararg tags: Tag): TagResourceRequest {
    return tagResourceRequestOf(resourceArn, tags.toList())
}
