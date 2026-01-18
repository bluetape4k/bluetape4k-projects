package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.CopyObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import io.bluetape4k.support.requireNotBlank
import java.net.URLEncoder

inline fun copyObjectRequestOf(
    srcBucket: String,
    srcKey: String,
    destBucket: String,
    destKey: String,
    acl: ObjectCannedAcl? = null,
    crossinline builder: CopyObjectRequest.Builder.() -> Unit = {},
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
