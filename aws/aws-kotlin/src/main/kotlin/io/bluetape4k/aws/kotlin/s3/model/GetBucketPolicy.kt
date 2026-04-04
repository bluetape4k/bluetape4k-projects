package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetBucketPolicyRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket]의 정책 조회를 위한 [GetBucketPolicyRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = getBucketPolicyRequestOf("my-bucket")
 * val response = s3Client.getBucketPolicy(request)
 * val policy = response.policy
 * ```
 *
 * @param bucket 버킷 이름
 * @param expectedBucketOwner 예상 버킷 소유자 계정 ID (null이면 생략)
 * @return [GetBucketPolicyRequest] 인스턴스
 */
inline fun getBucketPolicyRequestOf(
    bucket: String,
    expectedBucketOwner: String? = null,
    crossinline builder: GetBucketPolicyRequest.Builder.() -> Unit = {},
): GetBucketPolicyRequest {
    bucket.requireNotBlank("bucket")

    return GetBucketPolicyRequest {
        this.bucket = bucket
        this.expectedBucketOwner = expectedBucketOwner

        builder()
    }
}
