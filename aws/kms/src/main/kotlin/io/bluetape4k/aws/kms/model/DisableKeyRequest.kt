package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DisableKeyRequest

inline fun disableKeyRequest(
    @BuilderInference builder: DisableKeyRequest.Builder.() -> Unit,
): DisableKeyRequest =
    DisableKeyRequest.builder().apply(builder).build()

fun disableKeyRequestOf(keyId: String): DisableKeyRequest {
    keyId.requireNotBlank("keyId")

    return disableKeyRequest {
        keyId(keyId)
    }
}
