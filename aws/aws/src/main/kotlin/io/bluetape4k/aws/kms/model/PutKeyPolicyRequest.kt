package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest

/**
 * DSL 스타일의 빌더 람다로 [PutKeyPolicyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [PutKeyPolicyRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = putKeyPolicyRequest {
 *     keyId("key-id")
 *     policyName("default")
 * }
 * // request.policyName() == "default"
 * ```
 */
inline fun putKeyPolicyRequest(
    builder: PutKeyPolicyRequest.Builder.() -> Unit,
): PutKeyPolicyRequest =
    PutKeyPolicyRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [PutKeyPolicyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId], [policyName], [policy]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - 검증이 통과하면 세 필드를 빌더에 설정한 뒤 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = putKeyPolicyRequestOf(
 *     keyId = "key-id",
 *     policyName = "default",
 *     policy = """{"Version":"2012-10-17","Statement":[]}"""
 * )
 * // request.keyId() == "key-id"
 * ```
 */
fun putKeyPolicyRequestOf(
    keyId: String,
    policyName: String,
    policy: String,
    builder: PutKeyPolicyRequest.Builder.() -> Unit = {},
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
