package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.PutObjectRequest

inline fun putObjectRequest(
    bucket: String,
    key: String,
    initializer: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .apply(initializer)
        .build()
}

fun putObjectRequestOf(
    bucket: String,
    key: String,
    acl: String? = null,
    contentType: String? = null,
    initializer: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectRequest {
    return putObjectRequest(bucket, key) {
        acl(acl)
        contentType(contentType)
        initializer()
    }
}
