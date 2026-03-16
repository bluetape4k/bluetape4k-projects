package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.ListAliasesRequest

/**
 * DSL 스타일의 빌더 람다로 [ListAliasesRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [ListAliasesRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = listAliasesRequest {
 *     limit(10)
 * }
 * // request.limit() == 10
 * ```
 */
fun listAliasesRequest(
    builder: ListAliasesRequest.Builder.() -> Unit,
): ListAliasesRequest =
    ListAliasesRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [ListAliasesRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [keyId], [limit], [marker]는 `null`이 아닐 때만 빌더에 반영합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = listAliasesRequestOf(
 *     keyId = "key-id",
 *     limit = 20
 * )
 * // request.limit() == 20
 * ```
 */
fun listAliasesRequestOf(
    keyId: String? = null,
    limit: Int? = null,
    marker: String? = null,
    builder: ListAliasesRequest.Builder.() -> Unit = {},
): ListAliasesRequest {

    return listAliasesRequest {
        keyId?.let { keyId(it) }
        limit?.let { limit(it) }
        marker?.let { marker(it) }

        builder()
    }
}
