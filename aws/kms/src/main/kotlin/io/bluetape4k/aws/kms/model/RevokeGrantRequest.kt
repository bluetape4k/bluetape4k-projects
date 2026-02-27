package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.RevokeGrantRequest

inline fun revokeGrantRequest(
    @BuilderInference builder: RevokeGrantRequest.Builder.() -> Unit,
): RevokeGrantRequest =
    RevokeGrantRequest.builder().apply(builder).build()

fun revokeGrantRequestOf(
    keyId: String,
    grantId: String,
    @BuilderInference builder: RevokeGrantRequest.Builder.() -> Unit = {},
): RevokeGrantRequest {
    keyId.requireNotBlank("keyId")
    grantId.requireNotBlank("grantId")

    return revokeGrantRequest {
        keyId(keyId)
        grantId(grantId)

        builder()
    }
}
