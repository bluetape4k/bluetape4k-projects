package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.CreateKeyRequest
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType
import software.amazon.awssdk.services.kms.model.Tag

/**
 * DSL 스타일의 빌더 람다로 [CreateKeyRequest]를 생성합니다.
 *
 * @param builder [CreateKeyRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [CreateKeyRequest] 인스턴스.
 */
inline fun createKeyRequest(
    @BuilderInference builder: CreateKeyRequest.Builder.() -> Unit,
): CreateKeyRequest =
    CreateKeyRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [CreateKeyRequest]를 생성합니다.
 *
 * @param policy 키 정책(JSON 형식). null이면 기본 정책이 적용됩니다.
 * @param description 키에 대한 설명.
 * @param keyUsage 키 사용 유형 (예: [KeyUsageType.ENCRYPT_DECRYPT]).
 * @param keySpec 키 사양 (예: [KeySpec.SYMMETRIC_DEFAULT]).
 * @param origin 키 자료의 출처.
 * @param customKeyStoreId 커스텀 키 스토어 ID.
 * @param bypassPolicyLockoutSafetyCheck 정책 잠금 안전 검사 우회 여부.
 * @param tags 키에 연결할 태그 목록.
 * @param multiRegion 멀티 리전 키 여부.
 * @param xksKeyId 외부 키 스토어의 키 ID.
 * @param builder [CreateKeyRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [CreateKeyRequest] 인스턴스.
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
