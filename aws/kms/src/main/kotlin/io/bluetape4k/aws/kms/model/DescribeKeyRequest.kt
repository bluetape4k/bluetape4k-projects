package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest

inline fun describeKey(
    @BuilderInference builder: DescribeKeyRequest.Builder.() -> Unit,
): DescribeKeyRequest =
    DescribeKeyRequest.builder().apply(builder).build()

fun describeKeyOf(
    keyId: String,
    vararg grantTokens: String = emptyArray(),
    @BuilderInference builder: DescribeKeyRequest.Builder.() -> Unit = {},
): DescribeKeyRequest {
    keyId.requireNotBlank("keyId")

    return describeKey {
        keyId(keyId)
        if (grantTokens.isNotEmpty()) {
            grantTokens(*grantTokens)
        }

        builder()
    }
}
