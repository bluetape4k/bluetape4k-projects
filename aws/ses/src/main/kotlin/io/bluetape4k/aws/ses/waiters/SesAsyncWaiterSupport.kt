package io.bluetape4k.aws.ses.waiters

import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.waiters.SesAsyncWaiter
import java.util.concurrent.ScheduledExecutorService

/**
 * [SesAsyncWaiter.Builder]를 이용하여 [sesAsyncWaiter] 인스턴스를 생성합니다.
 *
 * ```
 * val waiter = sesAsyncWaiter {
 *    client(sesAsyncClient)
 *    scheduledExecutorService(scheduledExecutorService)
 *    overrideConfiguration(waiterOverrideConfiguration)
 *    maxAttempts(10)
 *    delay(10)
 *    maxBackoffTime(1000)
 *    backoffStrategy(ExponentialBackoffStrategy())
 *    acceptors(...)
 *    customWaiterBuilder(...)
 *    customWaiterParameters(...)
 *    customWaiterConfiguration(...)
 * }
 * waiter.waitUntil(...)
 * ```
 *
 * @param builder [SesAsyncWaiter.Builder] 초기화 람다
 * @return [sesAsyncWaiter] 인스턴스
 */
fun sesAsyncWaiter(
    @BuilderInference builder: SesAsyncWaiter.Builder.() -> Unit,
): SesAsyncWaiter {
    return SesAsyncWaiter.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

/**
 * [SesAsyncWaiter] 인스턴스를 생성합니다.
 *
 * ```
 * val waiter = sesAsyncWaiterOf(sesAsyncClient, scheduledExecutorService)
 * waiter.waitUntil(...)
 * ```
 *
 * @param client [SesAsyncClient] 인스턴스
 * @param scheduledExecutorService [ScheduledExecutorService] 인스턴스
 * @param configuration [WaiterOverrideConfiguration] 인스턴스
 * @return [SesAsyncWaiter] 인스턴스
 */
fun sesAsyncWaiterOf(
    client: SesAsyncClient,
    scheduledExecutorService: ScheduledExecutorService,
    configuration: WaiterOverrideConfiguration = waiterOverrideConfigurationOf(),
    @BuilderInference builder: SesAsyncWaiter.Builder.() -> Unit = {},
): SesAsyncWaiter = sesAsyncWaiter {
    client(client)
    scheduledExecutorService(scheduledExecutorService)
    overrideConfiguration(configuration)

    builder()
}
