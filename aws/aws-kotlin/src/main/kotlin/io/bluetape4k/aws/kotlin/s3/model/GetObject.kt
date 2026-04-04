package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket]의 [key]에 해당하는 객체를 조회하기 위한 [GetObjectRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = getObjectRequestOf(
 *     bucket = "my-bucket",
 *     key = "path/to/object.txt"
 * )
 * s3Client.getObject(request) { response ->
 *     response.body?.decodeToString()
 * }
 * ```
 *
 * @param bucket 버킷 이름
 * @param key 객체 키
 * @param versionId 특정 버전 ID (null이면 최신 버전)
 * @param partNumber 멀티파트 객체의 파트 번호 (null이면 전체)
 * @return [GetObjectRequest] 인스턴스
 */
inline fun getObjectRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    partNumber: Int? = null,
    crossinline builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRequest {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId
        this.partNumber = partNumber

        builder()
    }
}
