package io.bluetape4k.okio.coroutines

import okio.Timeout
import java.util.concurrent.TimeUnit

/**
 * [Timeout]이 설정된 경우 해당 시간 내에 [block]을 실행하고, 초과하면 null을 반환합니다.
 * [Timeout.NONE]이거나 타임아웃이 0이고 데드라인이 없으면 [block]을 직접 실행합니다.
 *
 * ```kotlin
 * val timeout = java.time.Duration.ofSeconds(1).toTimeout()
 * val result = withTimeoutOrNull(timeout) {
 *     "completed"
 * }
 * // result == "completed"
 *
 * val expired = java.time.Duration.ofMillis(1).toTimeout()
 * val timedOut = withTimeoutOrNull(expired) {
 *     kotlinx.coroutines.delay(1000)
 *     "never"
 * }
 * // timedOut == null
 * ```
 *
 * @param timeout 적용할 [Timeout] (NONE이면 제한 없음)
 * @param block 실행할 suspend 블록
 * @return 블록의 결과, 또는 타임아웃 시 null
 */
suspend inline fun <T: Any> withTimeoutOrNull(
    timeout: Timeout,
    crossinline block: suspend () -> T,
): T? {
    if (timeout == Timeout.NONE || (timeout.timeoutNanos() == 0L && !timeout.hasDeadline())) {
        return block()
    }

    val now = System.nanoTime()
    val waitNanos = when {
        timeout.timeoutNanos() != 0L && timeout.hasDeadline() -> minOf(
            timeout.timeoutNanos(),
            timeout.deadlineNanoTime() - now
        )

        timeout.timeoutNanos() != 0L -> timeout.timeoutNanos()
        timeout.hasDeadline()        -> timeout.deadlineNanoTime() - now
        else                         -> throw AssertionError("Unexpected Timeout state")
    }

    if (waitNanos <= 0L) {
        return null
    }

    val waitMillis = TimeUnit.NANOSECONDS.toMillis(waitNanos).let { millis ->
        if (millis <= 0L) 1L else millis
    }

    return kotlinx.coroutines.withTimeoutOrNull(waitMillis) {
        block()
    }
}
