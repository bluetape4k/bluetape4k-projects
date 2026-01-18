package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListTagsOfResourceRequest
import io.bluetape4k.support.requireNotBlank

inline fun listTagsOfResourceRequestOf(
    resourceArn: String,
    nextToken: String? = null,
    crossinline builder: ListTagsOfResourceRequest.Builder.() -> Unit = {},
): ListTagsOfResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return ListTagsOfResourceRequest {
        this.resourceArn = resourceArn
        this.nextToken = nextToken

        builder()
    }
}
