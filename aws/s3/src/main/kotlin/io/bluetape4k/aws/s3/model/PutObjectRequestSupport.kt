package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.PutObjectRequest

inline fun putObjectRequest(
    bucket: String,
    key: String,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .apply(builder)
        .build()
}

inline fun putObjectRequestOf(
    bucket: String,
    key: String,
    acl: String? = null,
    contentType: String? = null,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectRequest {
    return putObjectRequest(bucket, key) {
        acl?.let { acl(it) }
        contentType?.let { contentType(it) }

        builder()
    }
}
