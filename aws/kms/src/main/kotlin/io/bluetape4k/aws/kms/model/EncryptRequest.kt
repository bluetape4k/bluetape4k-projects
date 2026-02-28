package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.model.EncryptRequest

/**
 * DSL 스타일의 빌더 람다로 [EncryptRequest]를 생성합니다.
 *
 * @param builder [EncryptRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [EncryptRequest] 인스턴스.
 */
inline fun encryptRequest(
    @BuilderInference builder: EncryptRequest.Builder.() -> Unit,
): EncryptRequest =
    EncryptRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [EncryptRequest]를 생성합니다.
 *
 * @param keyId 암호화에 사용할 KMS 키의 ID 또는 ARN.
 * @param plainText 암호화할 평문 데이터([SdkBytes]).
 * @param encryptionContext 암호화 컨텍스트 (복호화 시 동일한 값이 필요).
 * @param grantTokens 암호화에 사용할 Grant 토큰 목록.
 * @param encryptionAlgorithm 암호화 알고리즘. null이면 키 기본 알고리즘이 사용됩니다.
 * @param dryRun 실제 암호화 없이 권한 검사만 수행할지 여부.
 * @param builder [EncryptRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [EncryptRequest] 인스턴스.
 */
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
