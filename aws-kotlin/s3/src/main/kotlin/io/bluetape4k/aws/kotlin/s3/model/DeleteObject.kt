package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectsRequest
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

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

inline fun deleteObjectsRequestOf(
    bucket: String,
    identifiers: List<ObjectIdentifier>,
    crossinline builder: DeleteObjectsRequest.Builder.() -> Unit,
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")
    identifiers.requireNotEmpty("identifiers")

    return DeleteObjectsRequest {
        this.bucket = bucket

        this.delete {
            this.objects = identifiers
        }

        builder()
    }
}

inline fun deleteObjectsRequestOf(
    bucket: String,
    delete: Delete,
    crossinline builder: DeleteObjectsRequest.Builder.() -> Unit,
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")

    return DeleteObjectsRequest {
        this.bucket = bucket
        this.delete = delete

        builder()
    }
}
