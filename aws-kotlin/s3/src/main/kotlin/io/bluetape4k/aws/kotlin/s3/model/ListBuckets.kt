package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.ListBucketsRequest

fun listBucketsRequestOf(
    maxBuckets: Int? = null,
    continuationToken: String? = null,
    @BuilderInference builder: ListBucketsRequest.Builder.() -> Unit = {},
): ListBucketsRequest =
    ListBucketsRequest {
        this.maxBuckets = maxBuckets
        this.continuationToken = continuationToken

        builder()
    }
