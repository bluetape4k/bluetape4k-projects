package io.bluetape4k.aws.s3.model


import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

inline fun deleteObjectRequest(
    bucket: String,
    key: String,
    @BuilderInference builder: DeleteObjectRequest.Builder.() -> Unit = {},
): DeleteObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .apply(builder)
        .build()
}

inline fun deleteObjectRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    @BuilderInference builder: DeleteObjectRequest.Builder.() -> Unit,
): DeleteObjectRequest {
    return deleteObjectRequest(bucket, key) {
        versionId?.run { versionId(this) }

        builder()
    }
}

inline fun deleteObjectsRequest(
    bucket: String,
    delete: Delete,
    @BuilderInference builder: DeleteObjectsRequest.Builder.() -> Unit = {},
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")
    return DeleteObjectsRequest.builder()
        .bucket(bucket)
        .delete(delete)
        .apply(builder)
        .build()
}

fun deleteObjectsRequestOf(
    bucket: String,
    delete: Delete,
    requestPlayer: String? = null,
): DeleteObjectsRequest {
    return deleteObjectsRequest(bucket, delete) {
        requestPlayer?.let { requestPayer(it) }
    }
}

fun deleteObjectsRequestOf(
    bucket: String,
    identifiers: Collection<ObjectIdentifier>,
    requestPlayer: String? = null,
): DeleteObjectsRequest {
    return deleteObjectsRequest(bucket, deleteOf(identifiers)) {
        requestPayer(requestPlayer)
    }
}
