package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.UntagResourceRequest
import io.bluetape4k.support.requireNotBlank

inline fun untagResourceRequestOf(
    resourceArn: String,
    tagKeys: List<String>,
    @BuilderInference crossinline builder: UntagResourceRequest.Builder.() -> Unit = {},
): UntagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return UntagResourceRequest {
        this.resourceArn = resourceArn
        this.tagKeys = tagKeys
        builder()
    }
}
