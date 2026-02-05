package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.UntagResourceRequest
import io.bluetape4k.support.requireNotBlank

fun untagResourceRequestOf(
    resourceArn: String,
    tagKeys: List<String>,
    @BuilderInference builder: UntagResourceRequest.Builder.() -> Unit = {},
): UntagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return UntagResourceRequest {
        this.resourceArn = resourceArn
        this.tagKeys = tagKeys
        builder()
    }
}
