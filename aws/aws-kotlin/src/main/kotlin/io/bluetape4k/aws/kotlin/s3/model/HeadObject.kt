package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket]의 [key]에 해당하는 객체 메타데이터 조회를 위한 [HeadObjectRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = headObjectRequestOf(
 *     bucket = "my-bucket",
 *     key = "path/to/object.txt"
 * )
 * val response = s3Client.headObject(request)
 * // response.contentLength — 객체 크기(바이트)
 * ```
 *
 * @param bucket 버킷 이름
 * @param key 객체 키
 * @return [HeadObjectRequest] 인스턴스
 */
inline fun headObjectRequestOf(
    bucket: String,
    key: String,
    crossinline builder: HeadObjectRequest.Builder.() -> Unit = {},
): HeadObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return HeadObjectRequest {
        this.bucket = bucket
        this.key = key

        builder()
    }
}
