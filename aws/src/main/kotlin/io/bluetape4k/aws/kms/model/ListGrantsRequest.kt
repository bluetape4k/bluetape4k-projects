package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.ListGrantsRequest

/**
 * DSL 스타일의 빌더 람다로 [ListGrantsRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [ListGrantsRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = listGrantsRequest {
 *     keyId("key-id")
 *     limit(10)
 * }
 * // request.limit() == 10
 * ```
 */
inline fun listGrantsRequest(
    builder: ListGrantsRequest.Builder.() -> Unit,
): ListGrantsRequest =
    ListGrantsRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [ListGrantsRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - [grantId], [marker], [limit]는 `null`이 아닐 때만 빌더에 반영합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = listGrantsRequestOf(
 *     keyId = "key-id",
 *     limit = 20
 * )
 * // request.keyId() == "key-id"
 * ```
 */
fun listGrantsRequestOf(
    keyId: String,
    grantId: String? = null,
    marker: String? = null,
    limit: Int? = null,
    builder: ListGrantsRequest.Builder.() -> Unit = {},
): ListGrantsRequest {
    keyId.requireNotBlank("keyId")

    return listGrantsRequest {
        keyId(keyId)
        grantId?.let { grantId(it) }
        marker?.let { marker(it) }
        limit?.let { limit(it) }

        builder()
    }
}
