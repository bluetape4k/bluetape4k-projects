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
    crossinline configurer: DeleteObjectRequest.Builder.() -> Unit = {},
): DeleteObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return DeleteObjectRequest.invoke {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        configurer()
    }
}

inline fun deleteObjectsRequestOf(
    bucket: String,
    identifiers: List<ObjectIdentifier>,
    crossinline configurer: DeleteObjectsRequest.Builder.() -> Unit,
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")
    identifiers.requireNotEmpty("identifiers")

    return DeleteObjectsRequest.invoke {
        this.bucket = bucket

        this.delete {
            this.objects = identifiers
        }

        configurer()
    }
}

inline fun deleteObjectsRequestOf(
    bucket: String,
    delete: Delete,
    crossinline configurer: DeleteObjectsRequest.Builder.() -> Unit,
): DeleteObjectsRequest {
    bucket.requireNotBlank("bucket")

    return DeleteObjectsRequest.invoke {
        this.bucket = bucket
        this.delete = delete

        configurer()
    }
}
