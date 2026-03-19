package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.EnableKeyRequest

/**
 * DSL 스타일의 빌더 람다로 [EnableKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [EnableKeyRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = enableKeyRequest {
 *     keyId("key-id")
 * }
 * // request.keyId() == "key-id"
 * ```
 */
inline fun enableKeyRequest(
    builder: EnableKeyRequest.Builder.() -> Unit,
): EnableKeyRequest =
    EnableKeyRequest.builder().apply(builder).build()

/**
 * 키 ID를 지정하여 [EnableKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - 검증이 통과하면 [EnableKeyRequest.Builder.keyId]에 값을 설정합니다.
 *
 * ```kotlin
 * val request = enableKeyRequestOf("key-id")
 * // request.keyId() == "key-id"
 * ```
 */
fun enableKeyRequestOf(keyId: String): EnableKeyRequest {
    keyId.requireNotBlank("keyId")

    return enableKeyRequest {
        keyId(keyId)
    }
}
