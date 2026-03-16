package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.ListKeysRequest

/**
 * DSL 스타일의 빌더 람다로 [ListKeysRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [ListKeysRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = listKeysRequest {
 *     limit(10)
 * }
 * // request.limit() == 10
 * ```
 */
inline fun listKeysRequest(
    @BuilderInference builder: ListKeysRequest.Builder.() -> Unit,
): ListKeysRequest =
    ListKeysRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [ListKeysRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [limit], [marker]는 `null`이 아닐 때만 빌더에 반영합니다.
 * - 마지막에 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = listKeysRequestOf(limit = 25)
 * // request.limit() == 25
 * ```
 */
fun listKeysRequestOf(
    limit: Int? = null,
    marker: String? = null,
    @BuilderInference builder: ListKeysRequest.Builder.() -> Unit = {},
): ListKeysRequest {

    return listKeysRequest {
        limit?.let { limit(it) }
        marker?.let { marker(it) }

        builder()
    }
}
