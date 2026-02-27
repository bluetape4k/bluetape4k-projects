package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.model.EncryptRequest

inline fun encryptRequest(
    @BuilderInference builder: EncryptRequest.Builder.() -> Unit,
): EncryptRequest =
    EncryptRequest.builder().apply(builder).build()

inline fun encryptRequestOf(
    keyId: String? = null,
    plainText: SdkBytes? = null,
    encryptionContext: Map<String, String>? = null,
    grantTokens: List<String>? = null,
    encryptionAlgorithm: String? = null,
    dryRun: Boolean? = null,
    @BuilderInference builder: EncryptRequest.Builder.() -> Unit = {},
): EncryptRequest = encryptRequest {

    keyId?.let { keyId(it) }
    plainText?.let { plaintext(it) }
    encryptionContext?.let { encryptionContext(it) }
    grantTokens?.let { grantTokens(it) }
    encryptionAlgorithm?.let { encryptionAlgorithm(it) }
    dryRun?.let { dryRun(it) }

    builder()
}
