package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetObjectAclRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [GetObjectAclRequest] 를 생성합니다.
 */
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
