package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.HeadBucketRequest
import io.bluetape4k.support.requireNotBlank

fun headBucketRequestOf(
    bucket: String,
    expectedBucketOwner: String? = null,
    @BuilderInference builder: HeadBucketRequest.Builder.() -> Unit = {},
): HeadBucketRequest {
    bucket.requireNotBlank("bucket")

    return HeadBucketRequest {
        this.bucket = bucket
        this.expectedBucketOwner = expectedBucketOwner

        builder()
    }
}
