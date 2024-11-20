package io.bluetape4k.aws.ses.waiters

import software.amazon.awssdk.core.retry.backoff.BackoffStrategy
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

inline fun WaiterOverrideConfiguration(
    initializer: WaiterOverrideConfiguration.Builder.() -> Unit,
): WaiterOverrideConfiguration {
    return WaiterOverrideConfiguration.builder().apply(initializer).build()
}

fun waiterOverrideConfigurationOf(
    maxAttempts: Int = 3,
    waitTimeout: Duration = 5.seconds,
    backoffStrategy: BackoffStrategy = BackoffStrategy.defaultStrategy(),
): WaiterOverrideConfiguration = WaiterOverrideConfiguration {
    this.backoffStrategy(backoffStrategy)
    this.maxAttempts(maxAttempts)
    this.waitTimeout(waitTimeout.toJavaDuration())
}
