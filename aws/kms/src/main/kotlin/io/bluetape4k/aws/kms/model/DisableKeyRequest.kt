package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DisableKeyRequest

/**
 * DSL 스타일의 빌더 람다로 [DisableKeyRequest]를 생성합니다.
 *
 * @param builder [DisableKeyRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [DisableKeyRequest] 인스턴스.
 */
inline fun disableKeyRequest(
    @BuilderInference builder: DisableKeyRequest.Builder.() -> Unit,
): DisableKeyRequest =
    DisableKeyRequest.builder().apply(builder).build()

/**
 * 키 ID를 지정하여 [DisableKeyRequest]를 생성합니다.
 *
 * 키를 비활성화하면 해당 키를 사용한 암호화/복호화 작업이 불가능해집니다.
 * [enableKeyRequestOf]로 다시 활성화할 수 있습니다.
 *
 * @param keyId 비활성화할 KMS 키의 ID 또는 ARN. 공백 불가.
 * @return 설정된 [DisableKeyRequest] 인스턴스.
 */
fun disableKeyRequestOf(keyId: String): DisableKeyRequest {
    keyId.requireNotBlank("keyId")

    return disableKeyRequest {
        keyId(keyId)
    }
}
