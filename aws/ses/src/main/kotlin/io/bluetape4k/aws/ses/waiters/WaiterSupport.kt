package io.bluetape4k.aws.ses.waiters

import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration
import software.amazon.awssdk.retries.api.BackoffStrategy
import java.time.Duration

fun waiterOverrideConfiguration(
    @BuilderInference builder: WaiterOverrideConfiguration.Builder.() -> Unit,
): WaiterOverrideConfiguration =
    WaiterOverrideConfiguration.builder().apply(builder).build()

fun waiterOverrideConfigurationOf(
    maxAttempts: Int = 3,
    waitTimeout: Duration = Duration.ofSeconds(5),
    backoffStrategy: BackoffStrategy = BackoffStrategy.fixedDelay(Duration.ofMillis(10)),
): WaiterOverrideConfiguration = waiterOverrideConfiguration {
    this.backoffStrategyV2(backoffStrategy)
    this.maxAttempts(maxAttempts)
    this.waitTimeout(waitTimeout)
}
