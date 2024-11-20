package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.GetObjectRequest

inline fun getObjectRequest(
    bucket: String,
    key: String,
    initializer: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .apply(initializer)
        .build()
}

fun getObjectRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    partNumber: Int? = null,
): GetObjectRequest {
    return getObjectRequest(bucket, key) {
        versionId?.run { versionId(this) }
        partNumber?.run { partNumber(this) }
    }
}
