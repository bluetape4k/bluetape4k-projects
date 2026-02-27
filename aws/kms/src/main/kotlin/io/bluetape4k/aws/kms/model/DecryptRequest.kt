package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.DryRunModifierType
import software.amazon.awssdk.services.kms.model.RecipientInfo

inline fun decryptRequest(
    @BuilderInference builder: DecryptRequest.Builder.() -> Unit,
): DecryptRequest =
    DecryptRequest.builder().apply(builder).build()


inline fun decryptRequestOf(
    ciphertextBlob: SdkBytes? = null,
    encryptionContext: Map<String, String>? = null,
    grantTokens: Collection<String>? = null,
    keyId: String? = null,
    encryptionAlgorithm: String? = null,
    recipient: RecipientInfo? = null,
    dryRun: Boolean? = null,
    dryRunModifiers: Collection<DryRunModifierType>? = null,
    @BuilderInference builder: DecryptRequest.Builder.() -> Unit = {},
): DecryptRequest = decryptRequest {

    ciphertextBlob?.let { ciphertextBlob(it) }
    encryptionContext?.let { encryptionContext(it) }
    grantTokens?.let { grantTokens(it) }
    keyId?.let { keyId(it) }
    encryptionAlgorithm?.let { encryptionAlgorithm(it) }
    recipient?.let { recipient(it) }
    dryRun?.let { dryRun(it) }
    dryRunModifiers?.let { dryRunModifiers(it) }

    builder()
}
