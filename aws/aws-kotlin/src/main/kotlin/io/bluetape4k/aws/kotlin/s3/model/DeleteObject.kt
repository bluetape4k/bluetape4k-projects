package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectsRequest
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

/**
 * [bucket]의 [key]에 해당하는 S3 객체를 삭제하기 위한 [DeleteObjectRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = deleteObjectRequestOf(
 *     bucket = "my-bucket",
 *     key = "path/to/object.txt"
 * )
 * s3Client.deleteObject(request)
 * ```
 *
 * @param bucket 버킷 이름
 * @param key 삭제할 객체 키
 * @param versionId 특정 버전 ID (null이면 최신 버전)
 * @return [DeleteObjectRequest] 인스턴스
 */
inline fun deleteObjectRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    crossinline builder: DeleteObjectRequest.Builder.() -> Unit = {},
): DeleteObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return DeleteObjectRequest {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        builder()
    }
}

/**
 * [bucket]의 [identifiers] 목록에 해당하는 S3 객체들을 일괄 삭제하기 위한 [DeleteObjectsRequest]를 생성합니다.
 *
 * ```kotlin
 * val identifiers = listOf("key1", "key2").map { it.toObjectIdentifier() }
 * val request = deleteObjectsRequestOf("my-bucket", identifiers)
 * s3Client.deleteObjects(request)
 * ```
 *
 * @param bucket 버킷 이름
 * @param identifiers 삭제할 객체 식별자 목록
 * @return [DeleteObjectsRequest] 인스턴스
 */
inline fun deleteObjectsRequestOf(
    bucket: String,
    identifiers: List<ObjectIdentifier>,
    crossinline builder: DeleteObjectsRequest.Builder.() -> Unit = {},
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")
    identifiers.requireNotEmpty("identifiers")

    return deleteObjectsRequestOf(bucket, deleteOf(identifiers), builder)
}

/**
 * [Delete] 객체를 사용하여 [DeleteObjectsRequest]를 생성합니다.
 *
 * ```kotlin
 * val delete = deleteOf("key1", "key2")
 * val request = deleteObjectsRequestOf("my-bucket", delete)
 * s3Client.deleteObjects(request)
 * ```
 *
 * @param bucket 버킷 이름
 * @param delete 삭제할 객체 목록을 포함하는 [Delete] 객체
 * @return [DeleteObjectsRequest] 인스턴스
 */
inline fun deleteObjectsRequestOf(
    bucket: String,
    delete: Delete,
    crossinline builder: DeleteObjectsRequest.Builder.() -> Unit = {},
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")

    return DeleteObjectsRequest {
        this.bucket = bucket
        this.delete = delete

        builder()
    }
}
