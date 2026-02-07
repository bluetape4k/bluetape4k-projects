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
    @BuilderInference crossinline builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRequest {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId
        this.partNumber = partNumber

        builder()
    }
}

inline fun getObjectAclRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    @BuilderInference crossinline builder: GetObjectAclRequest.Builder.() -> Unit = {},
): GetObjectAclRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectAclRequest {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        builder()
    }
}

inline fun getObjectRetentionRequestOf(
    bucket: String,
    key: String,
    versionId: String? = null,
    @BuilderInference crossinline builder: GetObjectRetentionRequest.Builder.() -> Unit = {},
): GetObjectRetentionRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return GetObjectRetentionRequest {
        this.bucket = bucket
        this.key = key
        this.versionId = versionId

        builder()
    }
}
