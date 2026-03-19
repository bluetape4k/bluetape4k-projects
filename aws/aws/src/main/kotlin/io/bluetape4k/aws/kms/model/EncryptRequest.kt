package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.model.EncryptRequest

/**
 * DSL 스타일의 빌더 람다로 [EncryptRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [EncryptRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = encryptRequest {
 *     keyId("key-id")
 * }
 * // request.keyId() == "key-id"
 * ```
 */
inline fun encryptRequest(
    builder: EncryptRequest.Builder.() -> Unit,
): EncryptRequest =
    EncryptRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [EncryptRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - 각 인자는 `null`이 아닐 때만 동일 이름의 빌더 메서드에 반영합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = encryptRequestOf(
 *     keyId = "key-id",
 *     plainText = SdkBytes.fromUtf8String("plain-text")
 * )
 * // request.keyId() == "key-id"
 * ```
 */
inline fun encryptRequestOf(
    keyId: String? = null,
    plainText: SdkBytes? = null,
    encryptionContext: Map<String, String>? = null,
    grantTokens: List<String>? = null,
    encryptionAlgorithm: String? = null,
    dryRun: Boolean? = null,
    builder: EncryptRequest.Builder.() -> Unit = {},
): EncryptRequest = encryptRequest {

    keyId?.let { keyId(it) }
    plainText?.let { plaintext(it) }
    encryptionContext?.let { encryptionContext(it) }
    grantTokens?.let { grantTokens(it) }
    encryptionAlgorithm?.let { encryptionAlgorithm(it) }
    dryRun?.let { dryRun(it) }

    builder()
}
