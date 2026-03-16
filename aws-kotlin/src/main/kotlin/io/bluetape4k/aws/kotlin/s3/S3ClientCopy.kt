package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CopyObjectRequest
import aws.sdk.kotlin.services.s3.model.CopyObjectResponse
import io.bluetape4k.aws.kotlin.s3.model.copyObjectRequestOf

/**
 * 원본 객체를 복사합니다.
 *
 * ```
 * val response = s3Client.copy("src-bucket", "src-key", "dest-bucket", "dest-key")
 * ```
 *
 * @param srcBucket 원본 버킷 이름
 * @param srcKey 원본 객체 키
 * @param destBucket 대상 버킷 이름
 * @param destKey 대상 객체 키
 * @return [CopyObjectResponse] 인스턴스
 */
suspend inline fun S3Client.copy(
    srcBucket: String,
    srcKey: String,
    destBucket: String,
    destKey: String,
    @BuilderInference crossinline builder: CopyObjectRequest.Builder.() -> Unit = {},
): CopyObjectResponse {
    val request = copyObjectRequestOf(srcBucket, srcKey, destBucket, destKey, builder = builder)
    return copyObject(request)
}
