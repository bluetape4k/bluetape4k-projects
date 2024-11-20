package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.ListBucketsRequest

inline fun listBucketsRequestOf(
    maxBuckets: Int? = null,
    continuationToken: String? = null,
    crossinline configurer: ListBucketsRequest.Builder.() -> Unit = {},
): ListBucketsRequest {
    return ListBucketsRequest {
        this.maxBuckets = maxBuckets
        this.continuationToken = continuationToken

        configurer()
    }
}
