package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.CopyObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import io.bluetape4k.support.requireNotBlank
import java.net.URLEncoder

/**
 * 버킷/키 정보를 받아 URL-encoded copy source를 생성한 뒤 [CopyObjectRequest] 를 생성합니다.
 */
inline fun copyObjectRequestOf(
    srcBucket: String,
    srcKey: String,
    destBucket: String,
    destKey: String,
    acl: ObjectCannedAcl? = null,
    @BuilderInference crossinline builder: CopyObjectRequest.Builder.() -> Unit = {},
): CopyObjectRequest {
    srcBucket.requireNotBlank("srcBucket")
    srcKey.requireNotBlank("srcKey")
    destBucket.requireNotBlank("destBucket")
    destKey.requireNotBlank("destKey")

    return CopyObjectRequest {
        this.copySource = URLEncoder.encode("$srcBucket/$srcKey", Charsets.UTF_8)
        this.bucket = destBucket
        this.key = destKey
        this.acl = acl

        builder()
    }
}

/**
 * 이미 구성된 copy source 문자열을 사용해 [CopyObjectRequest] 를 생성합니다.
 */
inline fun copyObjectRequestOf(
    copySource: String,
    destBucket: String,
    destKey: String,
    acl: ObjectCannedAcl? = null,
    @BuilderInference crossinline builder: CopyObjectRequest.Builder.() -> Unit = {},
): CopyObjectRequest {
    copySource.requireNotBlank("copySource")
    destBucket.requireNotBlank("destBucket")
    destKey.requireNotBlank("destKey")

    return CopyObjectRequest {
        this.copySource = copySource
        this.bucket = destBucket
        this.key = destKey
        this.acl = acl

        builder()
    }
}
