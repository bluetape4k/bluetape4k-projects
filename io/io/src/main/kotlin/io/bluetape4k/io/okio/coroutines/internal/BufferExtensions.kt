package io.bluetape4k.io.okio.coroutines.internal

import okio.Buffer
import okio.Timeout

/**
 * Buffer의 내용을 `cursor`를 통해 읽기 작업([block]) 수행합니다.
 */
inline fun <R> Buffer.readUnsafe(
    cursor: Buffer.UnsafeCursor = Buffer.UnsafeCursor(),
    block: (cursor: Buffer.UnsafeCursor) -> R,
): R {
    return readUnsafe(cursor).use {
        block(it)
    }
}

/**
 * Buffer의 내용을 `cursor`를 통해 읽기/쓰기 작업([block]) 수행합니다.
 */
inline fun <R> Buffer.readAndWriteUnsafe(
    cursor: Buffer.UnsafeCursor = Buffer.UnsafeCursor(),
    block: (cursor: Buffer.UnsafeCursor) -> R,
): R {
    return readAndWriteUnsafe(cursor).use { cursor ->
        block(cursor)
    }
}

// TODO: refactoring to Timeout.run
suspend inline fun <R: Any> withTimeoutOrNull(timeout: Timeout, crossinline block: suspend () -> R): R? {
    if (timeout.timeoutNanos() == 0L && !timeout.hasDeadline()) {
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
        else -> throw AssertionError()
    }

    return kotlinx.coroutines.withTimeoutOrNull((waitNanos / 1_000_000f).toLong()) {
        block()
    }
}
