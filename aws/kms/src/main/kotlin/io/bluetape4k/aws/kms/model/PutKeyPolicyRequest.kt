package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest

/**
 * DSL 스타일의 빌더 람다로 [PutKeyPolicyRequest]를 생성합니다.
 *
 * @param builder [PutKeyPolicyRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [PutKeyPolicyRequest] 인스턴스.
 */
inline fun putKeyPolicyRequest(
    @BuilderInference builder: PutKeyPolicyRequest.Builder.() -> Unit,
): PutKeyPolicyRequest =
    PutKeyPolicyRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [PutKeyPolicyRequest]를 생성합니다.
 *
 * @param keyId 정책을 설정할 KMS 키의 ID 또는 ARN. 공백 불가.
 * @param policyName 정책 이름. 현재 AWS KMS는 "default"만 지원합니다. 공백 불가.
 * @param policy JSON 형식의 키 정책 문서. 공백 불가.
 * @param builder [PutKeyPolicyRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [PutKeyPolicyRequest] 인스턴스.
 */
fun putKeyPolicyRequestOf(
    keyId: String,
    policyName: String,
    policy: String,
    @BuilderInference builder: PutKeyPolicyRequest.Builder.() -> Unit = {},
): PutKeyPolicyRequest {
    keyId.requireNotBlank("keyId")
    policyName.requireNotBlank("policyName")
    policy.requireNotBlank("policy")

    return putKeyPolicyRequest {
        keyId(keyId)
        policyName(policyName)
        policy(policy)

        builder()
    }
}
