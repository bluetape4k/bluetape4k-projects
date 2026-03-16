package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.GetObjectRequest

/**
 * [bucket], [key] 기반 [GetObjectRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = getObjectRequest("demo-bucket", "docs/readme.txt") { partNumber(2) }
 * // result.partNumber() == 2
 * ```
 */
inline fun getObjectRequest(
    bucket: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .apply(builder)
        .build()
}

/**
 * 선택 속성([versionId], [partNumber])을 포함해 [GetObjectRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = getObjectRequestOf("demo-bucket", "docs/readme.txt", versionId = "v2")
 * // result.versionId() == "v2"
 * ```
 */
fun getObjectRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    partNumber: Int? = null,
): GetObjectRequest =
    getObjectRequest(bucket, key) {
        versionId?.let { versionId(it) }
        partNumber?.let { partNumber(it) }
    }
