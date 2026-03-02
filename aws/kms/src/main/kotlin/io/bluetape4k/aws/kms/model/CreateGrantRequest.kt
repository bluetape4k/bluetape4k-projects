package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.CreateGrantRequest
import software.amazon.awssdk.services.kms.model.GrantOperation

/**
 * DSL 스타일의 빌더 람다로 [CreateGrantRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [CreateGrantRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = createGrantRequest {
 *     keyId("key-id")
 *     granteePrincipal("arn:aws:iam::111122223333:role/sample")
 * }
 * // request.keyId() == "key-id"
 * ```
 */
inline fun createGrantRequest(
    @BuilderInference builder: CreateGrantRequest.Builder.() -> Unit,
): CreateGrantRequest =
    CreateGrantRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [CreateGrantRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - [operations]가 비어있지 않을 때만 `operations(*operations)`를 호출합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = createGrantRequestOf(
 *     keyId = "key-id",
 *     granteePrincipal = "arn:aws:iam::111122223333:role/sample",
 *     GrantOperation.ENCRYPT
 * )
 * // request.operations().size == 1
 * ```
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
