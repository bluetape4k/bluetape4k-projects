package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

/**
 * 단일 객체 삭제용 [DeleteObjectRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = deleteObjectRequest("demo-bucket", "logs/app.log")
 * // result.key() == "logs/app.log"
 * ```
 */
inline fun deleteObjectRequest(
    bucket: String,
    key: String,
    builder: DeleteObjectRequest.Builder.() -> Unit = {},
): DeleteObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .apply(builder)
        .build()
}

/**
 * 선택적으로 [versionId]를 지정해 [DeleteObjectRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = deleteObjectRequestOf("demo-bucket", "logs/app.log", versionId = "3LgV") { }
 * // result.versionId() == "3LgV"
 * ```
 */
inline fun deleteObjectRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    builder: DeleteObjectRequest.Builder.() -> Unit,
): DeleteObjectRequest =
    deleteObjectRequest(bucket, key) {
        versionId?.run { versionId(this) }
        builder()
    }

/**
 * 다중 객체 삭제용 [DeleteObjectsRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val delete = deleteOf(objectIdentifier("a.txt"), objectIdentifier("b.txt"))
 * val result = deleteObjectsRequest("demo-bucket", delete)
 * // result.delete().objects().size == 2
 * ```
 */
inline fun deleteObjectsRequest(
    bucket: String,
    delete: Delete,
    builder: DeleteObjectsRequest.Builder.() -> Unit = {},
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")

    return DeleteObjectsRequest.builder()
        .bucket(bucket)
        .delete(delete)
        .apply(builder)
        .build()
}

/**
 * [Delete]와 선택 요청자 과금자([requestPlayer])로 [DeleteObjectsRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val delete = deleteOf(objectIdentifier("a.txt"))
 * val result = deleteObjectsRequestOf("demo-bucket", delete, requestPlayer = "requester")
 * // result.requestPayerAsString() == "requester"
 * ```
 */
fun deleteObjectsRequestOf(
    bucket: String,
    delete: Delete,
    requestPlayer: String? = null,
): DeleteObjectsRequest =
    deleteObjectsRequest(bucket, delete) {
        requestPlayer?.let { requestPayer(it) }
    }

/**
 * 식별자 컬렉션으로 [DeleteObjectsRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val ids = listOf(objectIdentifier("a.txt"), objectIdentifier("b.txt"))
 * val result = deleteObjectsRequestOf("demo-bucket", ids)
 * // result.delete().objects().size == 2
 * ```
 */
fun deleteObjectsRequestOf(
    bucket: String,
    identifiers: Collection<ObjectIdentifier>,
    requestPlayer: String? = null,
): DeleteObjectsRequest =
    deleteObjectsRequest(bucket, deleteOf(identifiers)) {
        requestPayer(requestPlayer)
    }
