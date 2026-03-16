package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.ListBucketsRequest

inline fun listBucketsRequestOf(
    maxBuckets: Int? = null,
    continuationToken: String? = null,
    @BuilderInference crossinline builder: ListBucketsRequest.Builder.() -> Unit = {},
): ListBucketsRequest =
    ListBucketsRequest {
        this.maxBuckets = maxBuckets
        this.continuationToken = continuationToken

        builder()
    }
