package io.bluetape4k.aws.sts.model

import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest

/**
 * DSL 블록으로 [GetSessionTokenRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `durationSeconds`, `serialNumber`, `tokenCode` 등을 설정할 수 있다.
 *
 * ```kotlin
 * val req = getSessionTokenRequest {
 *     durationSeconds(3600)
 * }
 * ```
 */
inline fun getSessionTokenRequest(
    @BuilderInference builder: GetSessionTokenRequest.Builder.() -> Unit,
): GetSessionTokenRequest =
    GetSessionTokenRequest.builder().apply(builder).build()

/**
 * 유효 시간(초)으로 [GetSessionTokenRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [durationSeconds]는 임시 자격 증명의 유효 시간(초)이다. 기본값은 3600초(1시간)이다.
 *
 * ```kotlin
 * val req = getSessionTokenRequestOf(durationSeconds = 7200)
 * // req.durationSeconds() == 7200
 * ```
 */
inline fun getSessionTokenRequestOf(
    durationSeconds: Int = 3600,
    @BuilderInference builder: GetSessionTokenRequest.Builder.() -> Unit = {},
): GetSessionTokenRequest =
    getSessionTokenRequest {
        durationSeconds(durationSeconds)

        builder()
    }
