package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.CreateGrantRequest
import software.amazon.awssdk.services.kms.model.GrantOperation

/**
 * DSL 스타일의 빌더 람다로 [CreateGrantRequest]를 생성합니다.
 *
 * @param builder [CreateGrantRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [CreateGrantRequest] 인스턴스.
 */
inline fun createGrantRequest(
    @BuilderInference builder: CreateGrantRequest.Builder.() -> Unit,
): CreateGrantRequest =
    CreateGrantRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [CreateGrantRequest]를 생성합니다.
 *
 * Grant는 특정 AWS 주체(principal)에게 KMS 키 작업 권한을 위임하는 메커니즘입니다.
 *
 * @param keyId Grant를 생성할 KMS 키의 ID 또는 ARN. 공백 불가.
 * @param granteePrincipal Grant를 받을 AWS 주체(IAM 사용자, 역할 등)의 ARN.
 * @param operations Grant로 허용할 KMS 작업 목록 (예: [GrantOperation.ENCRYPT], [GrantOperation.DECRYPT]).
 * @param builder [CreateGrantRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [CreateGrantRequest] 인스턴스.
 */
fun createGrantRequestOf(
    keyId: String,
    granteePrincipal: String,
    vararg operations: GrantOperation,
    @BuilderInference builder: CreateGrantRequest.Builder.() -> Unit = {},
): CreateGrantRequest {
    keyId.requireNotBlank("keyId")

    return createGrantRequest {
        keyId(keyId)
        granteePrincipal(granteePrincipal)
        if (operations.isNotEmpty()) {
            operations(*operations)
        }

        builder()
    }
}
