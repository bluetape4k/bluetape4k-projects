package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.ListKeysRequest

inline fun listKeysRequest(
    @BuilderInference builder: ListKeysRequest.Builder.() -> Unit,
): ListKeysRequest =
    ListKeysRequest.builder().apply(builder).build()

fun listKeysRequestOf(
    limit: Int? = null,
    marker: String? = null,
    @BuilderInference builder: ListKeysRequest.Builder.() -> Unit = {},
): ListKeysRequest {

    return listKeysRequest {
        limit?.let { limit(it) }
        marker?.let { marker(it) }

        builder()
    }
}
