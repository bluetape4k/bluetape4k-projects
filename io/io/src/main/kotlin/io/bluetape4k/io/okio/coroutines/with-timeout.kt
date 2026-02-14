package io.bluetape4k.io.okio.coroutines

import okio.Timeout
import java.util.concurrent.TimeUnit

/**
 * Okio 코루틴에서 `withTimeoutOrNull` 함수를 제공합니다.
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
        timeout.hasDeadline() -> timeout.deadlineNanoTime() - now
        else -> throw AssertionError("Unexpected Timeout state")
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
