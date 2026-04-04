package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.EncodingType
import aws.sdk.kotlin.services.s3.model.ListObjectsRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket]의 객체 목록 조회를 위한 [ListObjectsRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = listObjectsRequestOf(
 *     bucket = "my-bucket",
 *     prefix = "path/to/",
 *     maxKeys = 100
 * )
 * val response = s3Client.listObjects(request)
 * val objects = response.contents
 * ```
 *
 * @param bucket 버킷 이름
 * @param prefix 접두어 필터 (null이면 모든 객체)
 * @param delimiter 구분자 (null이면 계층 구조 없음)
 * @param maxKeys 반환할 최대 객체 수
 * @param encondingType 인코딩 타입
 * @return [ListObjectsRequest] 인스턴스
 */
inline fun listObjectsRequestOf(
    bucket: String,
    prefix: String? = null,
    delimiter: String? = null,
    maxKeys: Int? = null,
    encondingType: EncodingType? = null,
    crossinline builder: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest {
    bucket.requireNotBlank("bucket")

    return ListObjectsRequest {
        this.bucket = bucket
        this.prefix = prefix
        this.delimiter = delimiter
        this.maxKeys = maxKeys
        this.encodingType = encondingType

        builder()
    }
}
