package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.RevokeGrantRequest

/**
 * DSL 스타일의 빌더 람다로 [RevokeGrantRequest]를 생성합니다.
 *
 * @param builder [RevokeGrantRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [RevokeGrantRequest] 인스턴스.
 */
inline fun revokeGrantRequest(
    @BuilderInference builder: RevokeGrantRequest.Builder.() -> Unit,
): RevokeGrantRequest =
    RevokeGrantRequest.builder().apply(builder).build()

/**
 * 키 ID와 Grant ID를 지정하여 [RevokeGrantRequest]를 생성합니다.
 *
 * Grant를 취소하면 해당 Grant로 부여된 권한이 즉시 철회됩니다.
 *
 * @param keyId Grant가 연결된 KMS 키의 ID 또는 ARN. 공백 불가.
 * @param grantId 취소할 Grant의 ID. 공백 불가.
 * @param builder [RevokeGrantRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [RevokeGrantRequest] 인스턴스.
 */
fun revokeGrantRequestOf(
    keyId: String,
    grantId: String,
    @BuilderInference builder: RevokeGrantRequest.Builder.() -> Unit = {},
): RevokeGrantRequest {
    keyId.requireNotBlank("keyId")
    grantId.requireNotBlank("grantId")

    return revokeGrantRequest {
        keyId(keyId)
        grantId(grantId)

        builder()
    }
}
