package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.ListBucketsRequest

/**
 * S3 버킷 목록 조회를 위한 [ListBucketsRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = listBucketsRequestOf(maxBuckets = 100)
 * val response = s3Client.listBuckets(request)
 * val buckets = response.buckets
 * ```
 *
 * @param maxBuckets 반환할 최대 버킷 수 (null이면 기본값 사용)
 * @param continuationToken 페이지네이션 토큰 (null이면 첫 페이지)
 * @return [ListBucketsRequest] 인스턴스
 */
inline fun listBucketsRequestOf(
    maxBuckets: Int? = null,
    continuationToken: String? = null,
    crossinline builder: ListBucketsRequest.Builder.() -> Unit = {},
): ListBucketsRequest =
    ListBucketsRequest {
        this.maxBuckets = maxBuckets
        this.continuationToken = continuationToken

        builder()
    }
