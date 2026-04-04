package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.DeleteBucketRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket] 이름으로 [DeleteBucketRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = deleteBucketRequestOf("my-bucket")
 * s3Client.deleteBucket(request)
 * ```
 *
 * @param bucket 삭제할 버킷 이름
 * @param expectedBucketOwner 예상 버킷 소유자 계정 ID (null이면 생략)
 * @return [DeleteBucketRequest] 인스턴스
 */
inline fun deleteBucketRequestOf(
    bucket: String,
    expectedBucketOwner: String? = null,
    crossinline builder: DeleteBucketRequest.Builder.() -> Unit = {},
): DeleteBucketRequest {
    bucket.requireNotBlank("bucket")

    return DeleteBucketRequest {
        this.bucket = bucket
        this.expectedBucketOwner = expectedBucketOwner
        builder()
    }
}
