package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetBucketPolicyRequest
import io.bluetape4k.support.requireNotBlank

inline fun getBucketPolicyRequestOf(
    bucket: String,
    expectedBucketOwner: String? = null,
    @BuilderInference crossinline builder: GetBucketPolicyRequest.Builder.() -> Unit = {},
): GetBucketPolicyRequest {
    bucket.requireNotBlank("bucket")

    return GetBucketPolicyRequest {
        this.bucket = bucket
        this.expectedBucketOwner = expectedBucketOwner

        builder()
    }
}
