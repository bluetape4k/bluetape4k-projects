package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.CreateGrantRequest
import software.amazon.awssdk.services.kms.model.GrantOperation

inline fun createGrantRequest(
    @BuilderInference builder: CreateGrantRequest.Builder.() -> Unit,
): CreateGrantRequest =
    CreateGrantRequest.builder().apply(builder).build()

fun createGrantRequestOf(
    keyId: String,
    granteePrincipal: String,
    vararg operations: GrantOperation,
    @BuilderInference builder: CreateGrantRequest.Builder.() -> Unit = {},
): CreateGrantRequest {
    keyId.requireNotBlank("keyId")

    return createGrantRequest {
        keyId(keyId)
        granteePrincipal(granteePrincipal)
        if (operations.isNotEmpty()) {
            operations(*operations)
        }

        builder()
    }
}
