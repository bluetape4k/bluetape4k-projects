package io.bluetape4k.aws.ses.waiters

import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.waiters.SesWaiter

/**
 * [SesWaiter.Builder]를 이용하여 [SesWaiter] 인스턴스를 생성합니다.
 *
 * ```
 * val waiter = SesWaiter {
 *    client(sesClient)
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
 * @param builder [SesWaiter.Builder] 초기화 람다
 * @return [SesWaiter] 인스턴스
 */
inline fun SesWaiter(
    @BuilderInference builder: SesWaiter.Builder.() -> Unit,
): SesWaiter {
    return SesWaiter.builder().apply(builder).build()
}

/**
 * [SesWaiter] 인스턴스를 생성합니다.
 *
 * ```
 * val waiter = sesWaiterOf(sesClient)
 * waiter.waitUntil(...)
 * ```
 *
 * @param client [SesClient] 인스턴스
 * @param configuration [WaiterOverrideConfiguration] 인스턴스
 * @return [SesWaiter] 인스턴스
 */
fun sesWaiterOf(
    client: SesClient,
    configuration: WaiterOverrideConfiguration = waiterOverrideConfigurationOf(),
): SesWaiter = io.bluetape4k.aws.ses.waiters.SesWaiter {
    client(client)
    overrideConfiguration(configuration)
}
