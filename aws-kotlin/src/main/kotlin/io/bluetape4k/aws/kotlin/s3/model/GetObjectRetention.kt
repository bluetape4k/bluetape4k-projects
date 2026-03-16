package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.GetObjectRetentionRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [GetObjectRetentionRequest] 를 생성합니다.
 */
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
