package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.DeleteBucketRequest
import io.bluetape4k.support.requireNotBlank

inline fun deleteBucketRequestOf(
    bucket: String,
    expectedBucketOwner: String? = null,
    crossinline configurer: DeleteBucketRequest.Builder.() -> Unit = {},
): DeleteBucketRequest {
    bucket.requireNotBlank("bucket")

    return DeleteBucketRequest {
        this.bucket = bucket
        this.expectedBucketOwner = expectedBucketOwner
        configurer()
    }
}
