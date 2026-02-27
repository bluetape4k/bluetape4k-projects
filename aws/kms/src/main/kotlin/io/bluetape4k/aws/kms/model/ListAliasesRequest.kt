package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.ListAliasesRequest

fun listAliasesRequest(
    @BuilderInference builder: ListAliasesRequest.Builder.() -> Unit,
): ListAliasesRequest =
    ListAliasesRequest.builder().apply(builder).build()

fun listAliasesRequestOf(
    keyId: String? = null,
    limit: Int? = null,
    marker: String? = null,
    @BuilderInference builder: ListAliasesRequest.Builder.() -> Unit = {},
): ListAliasesRequest {

    return listAliasesRequest {
        keyId?.let { keyId(it) }
        limit?.let { limit(it) }
        marker?.let { marker(it) }

        builder()
    }
}
