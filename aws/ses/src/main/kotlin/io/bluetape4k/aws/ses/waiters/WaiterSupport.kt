package io.bluetape4k.aws.ses.waiters

import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration
import software.amazon.awssdk.retries.api.BackoffStrategy
import java.time.Duration

/**
 * DSL 블록으로 [WaiterOverrideConfiguration]을 빌드합니다.
 *
 * ```kotlin
 * val cfg = waiterOverrideConfiguration {
 *     maxAttempts(5)
 *     waitTimeout(Duration.ofSeconds(10))
 * }
 * ```
 */
fun waiterOverrideConfiguration(
    @BuilderInference builder: WaiterOverrideConfiguration.Builder.() -> Unit,
): WaiterOverrideConfiguration =
    WaiterOverrideConfiguration.builder().apply(builder).build()

/**
 * 기본값으로 [WaiterOverrideConfiguration]을 생성합니다.
 *
 * ## 동작/계약
 * - [maxAttempts] 기본값은 3, [waitTimeout] 기본값은 5초이다.
 * - [backoffStrategy] 기본값은 10ms 고정 지연이다.
 *
 * ```kotlin
 * val cfg = waiterOverrideConfigurationOf(maxAttempts = 5, waitTimeout = Duration.ofSeconds(10))
 * // cfg.maxAttempts().get() == 5
 * ```
 */
fun waiterOverrideConfigurationOf(
    maxAttempts: Int = 3,
    waitTimeout: Duration = Duration.ofSeconds(5),
    backoffStrategy: BackoffStrategy = BackoffStrategy.fixedDelay(Duration.ofMillis(10)),
): WaiterOverrideConfiguration = waiterOverrideConfiguration {
    this.backoffStrategyV2(backoffStrategy)
    this.maxAttempts(maxAttempts)
    this.waitTimeout(waitTimeout)
}
