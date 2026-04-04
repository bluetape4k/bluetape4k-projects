package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetObjectRetentionRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket]의 [key]에 해당하는 객체의 보존 설정 조회를 위한 [GetObjectRetentionRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = getObjectRetentionRequestOf(
 *     bucket = "my-bucket",
 *     key = "path/to/object.txt"
 * )
 * val response = s3Client.getObjectRetention(request)
 * ```
 *
 * @param bucket 버킷 이름
 * @param key 객체 키
 * @param versionId 특정 버전 ID (null이면 최신 버전)
 * @return [GetObjectRetentionRequest] 인스턴스
 */
inline fun getObjectRetentionRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    crossinline builder: GetObjectRetentionRequest.Builder.() -> Unit = {},
): GetObjectRetentionRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRetentionRequest {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        builder()
    }
}
