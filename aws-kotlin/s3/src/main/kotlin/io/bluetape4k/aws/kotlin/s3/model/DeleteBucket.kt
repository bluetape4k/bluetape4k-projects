package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.DeleteBucketRequest
import io.bluetape4k.support.requireNotBlank

fun deleteBucketRequestOf(
    bucket: String,
    expectedBucketOwner: String? = null,
    @BuilderInference builder: DeleteBucketRequest.Builder.() -> Unit = {},
): DeleteBucketRequest {
    bucket.requireNotBlank("bucket")

    return DeleteBucketRequest {
        this.bucket = bucket
        this.expectedBucketOwner = expectedBucketOwner
        builder()
    }
}
