package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.EnableKeyRequest

/**
 * DSL 스타일의 빌더 람다로 [EnableKeyRequest]를 생성합니다.
 *
 * @param builder [EnableKeyRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [EnableKeyRequest] 인스턴스.
 */
inline fun enableKeyRequest(
    @BuilderInference builder: EnableKeyRequest.Builder.() -> Unit,
): EnableKeyRequest =
    EnableKeyRequest.builder().apply(builder).build()

/**
 * 키 ID를 지정하여 [EnableKeyRequest]를 생성합니다.
 *
 * 비활성화된 키를 다시 활성화하여 암호화/복호화 작업에 사용할 수 있게 합니다.
 *
 * @param keyId 활성화할 KMS 키의 ID 또는 ARN. 공백 불가.
 * @return 설정된 [EnableKeyRequest] 인스턴스.
 */
fun enableKeyRequestOf(keyId: String): EnableKeyRequest {
    keyId.requireNotBlank("keyId")

    return enableKeyRequest {
        keyId(keyId)
    }
}
