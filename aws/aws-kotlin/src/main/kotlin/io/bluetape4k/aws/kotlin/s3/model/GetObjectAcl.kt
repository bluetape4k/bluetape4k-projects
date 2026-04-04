package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetObjectAclRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket]의 [key]에 해당하는 객체의 ACL 조회 요청인 [GetObjectAclRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = getObjectAclRequestOf(
 *     bucket = "my-bucket",
 *     key = "path/to/object.txt"
 * )
 * val response = s3Client.getObjectAcl(request)
 * ```
 *
 * @param bucket 버킷 이름
 * @param key 객체 키
 * @param versionId 특정 버전 ID (null이면 최신 버전)
 * @return [GetObjectAclRequest] 인스턴스
 */
inline fun getObjectAclRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    crossinline builder: GetObjectAclRequest.Builder.() -> Unit = {},
): GetObjectAclRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectAclRequest {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        builder()
    }
}
