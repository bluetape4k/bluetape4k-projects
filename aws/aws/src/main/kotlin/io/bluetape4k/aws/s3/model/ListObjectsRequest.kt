package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.ListObjectsRequest

/**
 * [bucket] 기반 [ListObjectsRequest]를 생성합니다.
 *
 * [bucket]은 공백일 수 없습니다.
 *
 * 예제:
 * ```kotlin
 * val result = listObjectsRequest("logs-bucket") { prefix("2026/") }
 * // result.prefix() == "2026/"
 * ```
 */
inline fun listObjectsRequest(
    bucket: String,
    builder: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest {
    bucket.requireNotBlank("bucket")
    return ListObjectsRequest.builder()
        .bucket(bucket)
        .apply(builder)
        .build()
}

/**
 * [listObjectsRequest]의 별칭 함수입니다.
 *
 * 예제:
 * ```kotlin
 * val result = listObjectsRequestOf("logs-bucket")
 * // result.bucket() == "logs-bucket"
 * ```
 */
inline fun listObjectsRequestOf(
    bucket: String,
    builder: ListObjectsRequest.Builder.() -> Unit = {},
): ListObjectsRequest =
    listObjectsRequest(bucket, builder)
