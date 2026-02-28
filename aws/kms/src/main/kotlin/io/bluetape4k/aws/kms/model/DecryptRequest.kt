package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.DryRunModifierType
import software.amazon.awssdk.services.kms.model.RecipientInfo

/**
 * DSL 스타일의 빌더 람다로 [DecryptRequest]를 생성합니다.
 *
 * @param builder [DecryptRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [DecryptRequest] 인스턴스.
 */
inline fun decryptRequest(
    @BuilderInference builder: DecryptRequest.Builder.() -> Unit,
): DecryptRequest =
    DecryptRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [DecryptRequest]를 생성합니다.
 *
 * @param ciphertextBlob 복호화할 암호문 데이터([SdkBytes]).
 * @param encryptionContext 암호화 시 사용한 암호화 컨텍스트 (동일한 값이어야 복호화 가능).
 * @param grantTokens 복호화에 사용할 Grant 토큰 목록.
 * @param keyId 복호화에 사용할 KMS 키의 ID 또는 ARN.
 * @param encryptionAlgorithm 암호화 알고리즘. null이면 키 기본 알고리즘이 사용됩니다.
 * @param recipient 수신자 정보 (Nitro Enclave 등 특수 환경에서 사용).
 * @param dryRun 실제 복호화 없이 권한 검사만 수행할지 여부.
 * @param dryRunModifiers dry run 동작 수정자 목록.
 * @param builder [DecryptRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [DecryptRequest] 인스턴스.
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
