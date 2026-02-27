package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.ListGrantsRequest

inline fun listGrantsRequest(
    @BuilderInference builder: ListGrantsRequest.Builder.() -> Unit,
): ListGrantsRequest =
    ListGrantsRequest.builder().apply(builder).build()

fun listGrantsRequestOf(
    keyId: String,
    grantId: String? = null,
    marker: String? = null,
    limit: Int? = null,
    @BuilderInference builder: ListGrantsRequest.Builder.() -> Unit = {},
): ListGrantsRequest {
    keyId.requireNotBlank("keyId")

    return listGrantsRequest {
        keyId(keyId)
        grantId?.let { grantId(it) }
        marker?.let { marker(it) }
        limit?.let { limit(it) }

        builder()
    }
}
