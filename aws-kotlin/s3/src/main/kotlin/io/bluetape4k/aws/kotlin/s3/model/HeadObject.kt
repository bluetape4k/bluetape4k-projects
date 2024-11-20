package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import io.bluetape4k.support.requireNotBlank

inline fun headObjectRequestOf(
    bucket: String,
    key: String,
    crossinline configurer: HeadObjectRequest.Builder.() -> Unit = {},
): HeadObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return HeadObjectRequest.invoke {
        this.bucket = bucket
        this.key = key
        configurer()
    }
}
