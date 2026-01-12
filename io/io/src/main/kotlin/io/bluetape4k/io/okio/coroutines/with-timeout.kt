package io.bluetape4k.io.okio.coroutines

import okio.Timeout

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

    return kotlinx.coroutines.withTimeoutOrNull((waitNanos / 1_000_000F).toLong()) {
        block()
    }
}
