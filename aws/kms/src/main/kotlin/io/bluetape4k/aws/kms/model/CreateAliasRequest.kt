package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.CreateAliasRequest

inline fun createAliasRequest(
    @BuilderInference builder: CreateAliasRequest.Builder.() -> Unit,
): CreateAliasRequest =
    CreateAliasRequest.builder().apply(builder).build()

fun createAliasRequestOf(
    aliasName: String,
    targetKeyId: String,
    @BuilderInference builder: CreateAliasRequest.Builder.() -> Unit = {},
): CreateAliasRequest {
    aliasName.requireNotBlank("aliasName")
    targetKeyId.requireNotBlank("targetKeyId")

    return createAliasRequest {
        aliasName(aliasName)
        targetKeyId(targetKeyId)

        builder()
    }
}
