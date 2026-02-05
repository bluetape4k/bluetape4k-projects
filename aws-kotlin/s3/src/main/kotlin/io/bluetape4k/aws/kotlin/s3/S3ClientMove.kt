package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.copyObject
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.model.CopyObjectRequest
import aws.sdk.kotlin.services.s3.model.CopyObjectResponse
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest

/**
 * S3 Object 를 이동 시킵니다.
 *
 * ```
 * val response = s3Client.move("src-bucket", "src-key", "dest-bucket", "dest-key")
 * ```
 *
 * @param srcBucket 이동할 Object 가 있는 버킷 이름
 * @param srcKey 이동할 Object 의 키
 * @param destBucket 이동할 Object 가 위치할 버킷 이름
 * @param destKey 이동할 Object 의 키
 * @return [CopyObjectResponse] 인스턴스
 */
suspend fun S3Client.move(
    srcBucket: String,
    srcKey: String,
    destBucket: String,
    destKey: String,
    @BuilderInference builder: CopyObjectRequest.Builder.() -> Unit = {},
): CopyObjectResponse {
    val response = copy(srcBucket, srcKey, destBucket, destKey, builder)

    if (response.copyObjectResult?.eTag?.isNotBlank() == true) {
        deleteObject {
            bucket = srcBucket
            key = srcKey
        }
    }
    return response
}

/**
 * S3 Object 를 이동 시킵니다.
 *
 * ```
 * val response = s3Client.move(
 *    copyRequestBuilder = {
 *        bucket = "dest-bucket"
 *        key = "dest-key"
 *        copySource = "src-bucket/src-key"
 *    },
 *    deleteRequestBuilder = {
 *          bucket = "src-bucket"
 *          key = "src-key"
 *    }
 * )
 * ```
 * @param copyRequestBuilder [CopyObjectRequest.Builder] 를 통해 [CopyObjectRequest] 를 설정합니다.
 * @param deleteRequestBuilder [DeleteObjectRequest.Builder] 를 통해 [DeleteObjectRequest] 를 설정합니다.
 * @return [CopyObjectResponse] 인스턴스
 */
suspend fun S3Client.move(
    @BuilderInference copyRequestBuilder: CopyObjectRequest.Builder.() -> Unit,
    @BuilderInference deleteRequestBuilder: DeleteObjectRequest.Builder.() -> Unit,
): CopyObjectResponse {
    val response = copyObject(copyRequestBuilder)

    if (response.copyObjectResult?.eTag?.isNotBlank() == true) {
        deleteObject(deleteRequestBuilder)
    }
    return response
}
