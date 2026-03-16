package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * [bucket], [key] 기반 [PutObjectRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = putObjectRequest("demo-bucket", "docs/readme.txt") { contentType("text/plain") }
 * // result.contentType() == "text/plain"
 * ```
 */
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

/**
 * 선택 속성([acl], [contentType])을 포함해 [PutObjectRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = putObjectRequestOf("demo-bucket", "docs/readme.txt", contentType = "text/plain")
 * // result.bucket() == "demo-bucket"
 * ```
 */
inline fun putObjectRequestOf(
    bucket: String,
    key: String,
    acl: String? = null,
    contentType: String? = null,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectRequest =
    putObjectRequest(bucket, key) {
        acl?.let { acl(it) }
        contentType?.let { contentType(it) }
        builder()
    }
