package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.EnableKeyRequest

inline fun enableKeyRequest(
    @BuilderInference builder: EnableKeyRequest.Builder.() -> Unit,
): EnableKeyRequest =
    EnableKeyRequest.builder().apply(builder).build()

fun enableKeyRequestOf(keyId: String): EnableKeyRequest {
    keyId.requireNotBlank("keyId")

    return enableKeyRequest {
        keyId(keyId)
    }
}
