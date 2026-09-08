package io.bluetape4k.testcontainers

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

/**
 * Docker/Testcontainers 자원 정리를 제한시간 안에서 수행합니다.
 *
 * ## 동작/계약
 * - 정리 작업은 호출자와 분리된 virtual thread에서 실행됩니다.
 * - 제한시간을 넘기면 작업을 interrupt하고 [TimeoutException]을 던집니다.
 * - 정리 작업이 예외를 던지면 원래 예외를 그대로 전파합니다.
 * - 호출자가 interrupt된 경우 interrupt 상태를 복원한 뒤 예외를 전파합니다.
 *
 * Docker API 호출이 응답하지 않는 경우에도 테스트 JVM이 무기한 정리 작업을 기다리지 않도록
 * 컨테이너 종료와 네트워크 정리에 사용합니다.
 */
internal inline fun runCleanupWithin(
    timeout: Duration,
    crossinline cleanup: () -> Unit,
) {
    require(timeout.isFinite() && timeout.isPositive()) {
        "Cleanup timeout must be finite and positive: [$timeout]"
    }

    // ExecutorService.close()는 완료되지 않은 작업을 무기한 기다립니다.
    // Docker API 호출이 interrupt를 무시할 수 있으므로 호출자의 deadline 이후에는
    // `use` 대신 기다리지 않는 방식으로 executor를 종료합니다.
    val executor = Executors.newVirtualThreadPerTaskExecutor()
    var failure: Throwable? = null
    try {
        val future = executor.submit { cleanup() }
        try {
            future.get(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            failure = e
        } catch (e: TimeoutException) {
            future.cancel(true)
            failure = e
        } catch (e: ExecutionException) {
            failure = e.cause ?: e
        }
    } finally {
        executor.shutdownNow()
    }

    if (failure != null) {
        throw failure
    }
}
