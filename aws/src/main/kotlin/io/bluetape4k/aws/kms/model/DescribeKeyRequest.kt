package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest

/**
 * DSL 스타일의 빌더 람다로 [DescribeKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [DescribeKeyRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = describeKey {
 *     keyId("key-id")
 * }
 * // request.keyId() == "key-id"
 * ```
 */
inline fun describeKey(
    builder: DescribeKeyRequest.Builder.() -> Unit,
): DescribeKeyRequest =
    DescribeKeyRequest.builder().apply(builder).build()

/**
 * 키 ID를 지정하여 [DescribeKeyRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - [grantTokens]가 비어있지 않을 때만 `grantTokens(*grantTokens)`를 호출합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = describeKeyOf(
 *     keyId = "alias/sample",
 *     "grant-token-1"
 * )
 * // request.grantTokens().size == 1
 * ```
 */
fun describeKeyOf(
    keyId: String,
    vararg grantTokens: String = emptyArray(),
    builder: DescribeKeyRequest.Builder.() -> Unit = {},
): DescribeKeyRequest {
    keyId.requireNotBlank("keyId")

    return describeKey {
        keyId(keyId)
        if (grantTokens.isNotEmpty()) {
            grantTokens(*grantTokens)
        }

        builder()
    }
}
