package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.RevokeGrantRequest

/**
 * DSL 스타일의 빌더 람다로 [RevokeGrantRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [RevokeGrantRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = revokeGrantRequest {
 *     keyId("key-id")
 *     grantId("grant-id")
 * }
 * // request.grantId() == "grant-id"
 * ```
 */
inline fun revokeGrantRequest(
    @BuilderInference builder: RevokeGrantRequest.Builder.() -> Unit,
): RevokeGrantRequest =
    RevokeGrantRequest.builder().apply(builder).build()

/**
 * 키 ID와 Grant ID를 지정하여 [RevokeGrantRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId], [grantId]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - 검증이 통과하면 두 필드를 설정하고 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = revokeGrantRequestOf(
 *     keyId = "key-id",
 *     grantId = "grant-id"
 * )
 * // request.keyId() == "key-id"
 * ```
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
