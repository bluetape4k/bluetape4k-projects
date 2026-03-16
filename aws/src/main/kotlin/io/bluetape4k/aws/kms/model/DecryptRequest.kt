package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.DryRunModifierType
import software.amazon.awssdk.services.kms.model.RecipientInfo

/**
 * DSL 스타일의 빌더 람다로 [DecryptRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [DecryptRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = decryptRequest {
 *     keyId("key-id")
 * }
 * // request.keyId() == "key-id"
 * ```
 */
inline fun decryptRequest(
    builder: DecryptRequest.Builder.() -> Unit,
): DecryptRequest =
    DecryptRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [DecryptRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - 각 인자는 `null`이 아닐 때만 동일 이름의 빌더 메서드에 반영합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = decryptRequestOf(
 *     keyId = "key-id",
 *     ciphertextBlob = SdkBytes.fromUtf8String("cipher-text")
 * )
 * // request.keyId() == "key-id"
 * ```
 */
inline fun decryptRequestOf(
    ciphertextBlob: SdkBytes? = null,
    encryptionContext: Map<String, String>? = null,
    grantTokens: Collection<String>? = null,
    keyId: String? = null,
    encryptionAlgorithm: String? = null,
    recipient: RecipientInfo? = null,
    dryRun: Boolean? = null,
    dryRunModifiers: Collection<DryRunModifierType>? = null,
    builder: DecryptRequest.Builder.() -> Unit = {},
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
