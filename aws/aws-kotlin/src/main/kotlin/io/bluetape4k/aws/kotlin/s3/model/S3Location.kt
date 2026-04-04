package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.S3Location
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket] 이름으로 [S3Location]을 생성합니다.
 *
 * ```kotlin
 * val location = s3LocationOf("my-bucket") {
 *     prefix = "path/to/"
 * }
 * ```
 *
 * @param bucket 버킷 이름
 * @return [S3Location] 인스턴스
 */
inline fun s3LocationOf(
    bucket: String,
    crossinline builder: S3Location.Builder.() -> Unit = {},
): S3Location {
    bucket.requireNotBlank("bucket")

    return S3Location.invoke {
        this.bucketName = bucket

        builder()
    }
}
