package io.bluetape4k.aws.sts.model

import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest

/**
 * DSL 블록으로 [GetCallerIdentityRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 추가 설정을 적용할 수 있다.
 * - 파라미터가 없는 요청이므로 대부분의 경우 빈 블록으로 사용한다.
 *
 * ```kotlin
 * val req = getCallerIdentityRequest {}
 * ```
 */
inline fun getCallerIdentityRequest(
    builder: GetCallerIdentityRequest.Builder.() -> Unit,
): GetCallerIdentityRequest =
    GetCallerIdentityRequest.builder().apply(builder).build()
