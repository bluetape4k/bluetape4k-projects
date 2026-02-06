package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.ListObjectsRequest

inline fun listObjectsRequest(
    bucket: String,
    @BuilderInference builder: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest {
    bucket.requireNotBlank("bucket")
    return ListObjectsRequest.builder()
        .bucket(bucket)
        .apply(builder)
        .build()
}

inline fun listObjectsRequestOf(
    bucket: String,
    @BuilderInference builder: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest {
    return listObjectsRequest(bucket, builder)
}
