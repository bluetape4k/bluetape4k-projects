package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import io.bluetape4k.support.requireNotBlank

/**
 * [bucket]의 [key]에 객체를 저장하기 위한 [PutObjectRequest] 를 생성합니다.
 */
inline fun putObjectRequestOf(
    bucket: String,
    key: String,
    body: ByteStream? = null,
    metadata: Map<String, String>? = null,
    acl: ObjectCannedAcl? = null,
    contentType: String? = null,
    crossinline builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return PutObjectRequest {
        this.bucket = bucket
        this.key = key
        this.body = body
        this.metadata = metadata
        this.acl = acl
        this.contentType = contentType

        builder()
    }
}
