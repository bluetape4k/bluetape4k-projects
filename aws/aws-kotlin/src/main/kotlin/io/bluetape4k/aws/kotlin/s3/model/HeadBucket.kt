package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.HeadBucketRequest
import io.bluetape4k.support.requireNotBlank

/**
 * 버킷 존재 여부 확인을 위한 [HeadBucketRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = headBucketRequestOf("my-bucket")
 * val response = s3Client.headBucket(request)
 * ```
 *
 * @param bucket 버킷 이름
 * @param expectedBucketOwner 예상 버킷 소유자 계정 ID (null이면 생략)
 * @return [HeadBucketRequest] 인스턴스
 */
inline fun headBucketRequestOf(
    bucket: String,
    expectedBucketOwner: String? = null,
    crossinline builder: HeadBucketRequest.Builder.() -> Unit = {},
): HeadBucketRequest {
    bucket.requireNotBlank("bucket")

    return HeadBucketRequest {
        this.bucket = bucket
        this.expectedBucketOwner = expectedBucketOwner

        builder()
    }
}
