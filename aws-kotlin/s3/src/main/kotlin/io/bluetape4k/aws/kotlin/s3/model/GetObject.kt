package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetObjectAclRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRetentionRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [GetObjectRequest] 를 생성합니다.
 */
inline fun getObjectRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    partNumber: Int? = null,
    crossinline configurer: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRequest.invoke {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId
        this.partNumber = partNumber

        configurer()
    }
}

inline fun getObjectAclRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    crossinline configurer: GetObjectAclRequest.Builder.() -> Unit = {},
): GetObjectAclRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectAclRequest.invoke {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        configurer()
    }
}

inline fun getObjectRetentionRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    crossinline configurer: GetObjectRetentionRequest.Builder.() -> Unit = {},
): GetObjectRetentionRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRetentionRequest.invoke {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        configurer()
    }
}
