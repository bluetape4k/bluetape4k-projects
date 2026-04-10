package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchWriter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * Writer 타임아웃 발생 시 던지는 예외.
 *
 * [kotlinx.coroutines.TimeoutCancellationException]을 래핑하여
 * retry/skip 경로로 전달한다 (CancellationException 서브타입이 아님).
 */
internal class WriteTimeoutException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

/**
 * [writer.write]를 [timeout] 시간 내에 실행한다.
 *
 * - [timeout]이 0 이하면 타임아웃을 적용하지 않는다.
 * - [TimeoutCancellationException]은 [WriteTimeoutException]으로 변환하여
 *   외부 취소([kotlinx.coroutines.CancellationException])와 구분한다.
 */
@Suppress("UNCHECKED_CAST")
internal suspend fun writeWithTimeout(
    writer: BatchWriter<*>,
    items: List<Any>,
    timeout: Duration,
) {
    val w = writer as BatchWriter<Any>
    if (timeout.isPositive()) {
        try {
            withTimeout(timeout) { w.write(items) }
        } catch (e: TimeoutCancellationException) {
            throw WriteTimeoutException(
                "Writer timed out after $timeout",
                e,
            )
        }
    } else {
        w.write(items)
    }
}
