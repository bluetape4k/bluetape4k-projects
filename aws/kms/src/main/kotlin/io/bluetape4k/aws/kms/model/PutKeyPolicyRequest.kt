package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest

inline fun putKeyPolicyRequest(
    @BuilderInference builder: PutKeyPolicyRequest.Builder.() -> Unit,
): PutKeyPolicyRequest =
    PutKeyPolicyRequest.builder().apply(builder).build()

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
