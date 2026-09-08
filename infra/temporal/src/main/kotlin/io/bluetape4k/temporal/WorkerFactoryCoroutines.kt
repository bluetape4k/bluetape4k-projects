package io.bluetape4k.temporal

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireGe
import io.temporal.worker.WorkerFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

private object WorkerFactoryTemporalLog : KLoggingChannel()

/**
 * [WorkerFactory]를 제한된 시간 안에 graceful shutdown합니다.
 *
 * 먼저 [WorkerFactory.shutdown]을 한 번 요청하고 [timeout]까지 한 번 기다립니다. 시간 안에
 * 종료되지 않았고 [force]가 `true`이면 [WorkerFactory.shutdownNow]를 요청한 뒤 추가로 무제한
 * 대기하지 않습니다. 대기 중 호출자가 취소되면 취소가 관찰된 뒤 강제 종료로 승격하지
 * 않습니다. 이 확장은 workflow client나 service stubs의 소유권을 갖지 않으므로 닫지 않습니다.
 *
 * @param timeout graceful shutdown 대기 시간 (유한한 0 이상)
 * @param force 시간 초과 후 강제 종료 요청 여부
 * @return 호출 시점의 worker factory 종료 여부
 */
@Suppress("TooGenericExceptionCaught") // SDK 예외 원문은 로그에 남기지 않고 호출자에게 그대로 전달합니다.
suspend fun WorkerFactory.shutdownSuspending(timeout: Duration, force: Boolean = false): Boolean {
    require(timeout.isFinite()) { "timeout must be finite." }
    val validTimeout = timeout.requireGe(Duration.ZERO, "timeout")
    return withContext(Dispatchers.IO) {
        try {
            if (!isShutdown) {
                shutdown()
            }
            awaitTermination(validTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            coroutineContext.ensureActive()

            if (!isTerminated && force) {
                shutdownNow()
            }

            isTerminated.also { terminated ->
                WorkerFactoryTemporalLog.log.debug(
                    "Temporal worker factory shutdown status={}",
                    if (terminated) "terminated" else "pending",
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WorkerFactoryTemporalLog.log.warn("Temporal worker factory shutdown status=failure")
            throw e
        }
    }
}
