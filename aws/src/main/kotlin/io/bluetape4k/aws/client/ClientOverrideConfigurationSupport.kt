package io.bluetape4k.aws.client

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration

/**
 * [ClientOverrideConfiguration]을 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - [ClientOverrideConfiguration.builder]로 생성한 빌더에 [builder]를 적용한다.
 * - [builder]의 설정 결과를 `build()`로 고정해 반환한다.
 *
 * ```kotlin
 * val configuration = clientOverrideConfiguration {
 *     apiCallAttemptTimeout(java.time.Duration.ofSeconds(1))
 * }
 * // configuration.apiCallAttemptTimeout().isPresent == true
 * ```
 */
inline fun clientOverrideConfiguration(
    builder: ClientOverrideConfiguration.Builder.() -> Unit,
): ClientOverrideConfiguration {
    return ClientOverrideConfiguration.builder().apply(builder).build()
}
