package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DisableKeyRequest

/**
 * DSL 스타일의 빌더 람다로 [DisableKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [DisableKeyRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = disableKeyRequest {
 *     keyId("arn:aws:kms:ap-northeast-2:111122223333:key/abcd")
 * }
 * // request.keyId().contains("key/") == true
 * ```
 */
inline fun disableKeyRequest(
    builder: DisableKeyRequest.Builder.() -> Unit,
): DisableKeyRequest =
    DisableKeyRequest.builder().apply(builder).build()

/**
 * 키 ID를 지정하여 [DisableKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - 검증이 통과하면 [DisableKeyRequest.Builder.keyId]에 값을 설정합니다.
 *
 * ```kotlin
 * val request = disableKeyRequestOf("key-id")
 * // request.keyId() == "key-id"
 * ```
 *
 */
fun disableKeyRequestOf(keyId: String): DisableKeyRequest {
    keyId.requireNotBlank("keyId")

    return disableKeyRequest {
        keyId(keyId)
    }
}
