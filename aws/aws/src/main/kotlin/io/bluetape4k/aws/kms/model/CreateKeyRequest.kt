package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.CreateKeyRequest
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType
import software.amazon.awssdk.services.kms.model.Tag

/**
 * DSL 스타일의 빌더 람다로 [CreateKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [CreateKeyRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = createKeyRequest {
 *     description("sample key")
 * }
 * // request.description() == "sample key"
 * ```
 */
inline fun createKeyRequest(
    builder: CreateKeyRequest.Builder.() -> Unit,
): CreateKeyRequest =
    CreateKeyRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [CreateKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - 각 인자는 `null`이 아닐 때만 동일 이름의 빌더 메서드에 반영합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = createKeyRequestOf(
 *     description = "application key",
 *     keyUsage = KeyUsageType.ENCRYPT_DECRYPT,
 *     keySpec = KeySpec.SYMMETRIC_DEFAULT
 * )
 * // request.keySpec() == KeySpec.SYMMETRIC_DEFAULT
 * ```
 */
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
    builder: CreateKeyRequest.Builder.() -> Unit = {},
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
