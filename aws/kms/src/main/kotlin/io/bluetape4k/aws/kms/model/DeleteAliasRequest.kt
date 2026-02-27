package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DeleteAliasRequest

inline fun deleteAlias(
    @BuilderInference builder: DeleteAliasRequest.Builder.() -> Unit,
): DeleteAliasRequest =
    DeleteAliasRequest.builder().apply(builder).build()

fun deleteAliasOf(aliasName: String): DeleteAliasRequest {
    aliasName.requireNotBlank("aliasName")
    return deleteAlias {
        aliasName(aliasName)
    }
}
