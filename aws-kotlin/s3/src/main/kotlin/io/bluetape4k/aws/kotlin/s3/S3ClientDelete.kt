package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObjects
import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.DeleteObjectsRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectsResponse
import io.bluetape4k.aws.kotlin.s3.model.deleteOf
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

/**
 * S3 Bucket 에서 여러 Object 를 삭제합니다.
 *
 * ```
 * val response = s3Client.deleteAll("bucket", "key-1", "key-2")
 * ```
 *
 * @param bucket 삭제할 Object 가 있는 버킷 이름
 * @param keys 삭제할 Object 의 키 목록
 * @return [DeleteObjectsResponse] 인스턴스
 */
suspend fun S3Client.deleteAll(
    bucket: String,
    vararg keys: String,
    @BuilderInference builder: Delete.Builder.() -> Unit = {},
): DeleteObjectsResponse {
    bucket.requireNotBlank("bucketName")

    return deleteObjects {
        this.bucket = bucket
        this.delete = deleteOf(*keys, builder = builder)
    }
}

/**
 * S3 [bucket]의 [keys]에 해당하는 오브젝트들을 삭제합니다.
 *
 * ```
 * val response = s3Client.deleteAll("bucket", listOf("key-1", "key-2"))
 * ```
 *
 * @param bucket 삭제할 Object 가 있는 버킷 이름
 * @param keys 삭제할 Object 의 키 목록
 * @return [DeleteObjectsResponse] 인스턴스
 */
suspend fun S3Client.deleteAll(
    bucket: String,
    keys: Collection<String>,
    @BuilderInference bulider: Delete.Builder.() -> Unit = {},
): DeleteObjectsResponse {
    bucket.requireNotBlank("bucketName")
    keys.requireNotEmpty("keys")
    log.debug { "Delete all objects in bucket=$bucket" }

    return deleteObjects {
        this.bucket = bucket
        this.delete = deleteOf(keys, builder = bulider)
    }
}

/**
 * S3 Bucket 에서 여러 Object 를 삭제합니다.
 *
 * ```
 * val keys = listOf("key-1", "key-2")
 * val response = s3Client.deleteAll("bucket-1") {
 *      delete {
 *             quiet = true
 *             this.objects = keys.map { it.toObjectIdentifier() }
 *      }
 * }
 * ```
 *
 * @param bucket 삭제할 Object 가 있는 버킷 이름
 * @param builder [DeleteObjectsRequest.Builder]를 통해 [DeleteObjectsRequest]를 설정합니다.
 * @return [DeleteObjectsResponse] 인스턴스
 */
suspend fun S3Client.deleteAll(
    bucket: String,
    @BuilderInference builder: DeleteObjectsRequest.Builder.() -> Unit,
): DeleteObjectsResponse {
    bucket.requireNotBlank("bucketName")

    return deleteObjects {
        this.bucket = bucket
        builder()
    }
}
