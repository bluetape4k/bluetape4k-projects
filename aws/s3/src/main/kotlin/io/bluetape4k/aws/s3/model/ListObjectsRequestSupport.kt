package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.ListObjectsRequest

inline fun listObjectsRequest(
    bucket: String,
    initializer: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest {
    bucket.requireNotBlank("bucket")
    return ListObjectsRequest.builder()
        .bucket(bucket)
        .apply(initializer)
        .build()
}

fun listObjectsRequestOf(
    bucket: String,
    initializer: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest {
    return listObjectsRequest(bucket, initializer)
}
