package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.CreateKeyRequest
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType
import software.amazon.awssdk.services.kms.model.Tag

inline fun createKeyRequest(
    @BuilderInference builder: CreateKeyRequest.Builder.() -> Unit,
): CreateKeyRequest =
    CreateKeyRequest.builder().apply(builder).build()

inline fun createKeyRequestOf(
    policy: String? = null,
    description: String? = null,
    keyUsage: KeyUsageType? = null,
    keySpec: KeySpec? = null,
    origin: String? = null,
    customKeyStoreId: String? = null,
    bypassPolicyLockoutSafetyCheck: Boolean? = null,
    tags: List<Tag>? = null,
    multiRegion: Boolean? = null,
    xksKeyId: String? = null,
    @BuilderInference builder: CreateKeyRequest.Builder.() -> Unit = {},
): CreateKeyRequest = createKeyRequest {

    policy?.let { policy(it) }
    description?.let { description(it) }
    keyUsage?.let { keyUsage(it) }
    keySpec?.let { keySpec(it) }
    origin?.let { origin(it) }
    customKeyStoreId?.let { customKeyStoreId(it) }
    bypassPolicyLockoutSafetyCheck?.let { bypassPolicyLockoutSafetyCheck(it) }
    tags?.let { tags(it) }
    multiRegion?.let { multiRegion(it) }
    xksKeyId?.let { xksKeyId(it) }

    builder()
}
