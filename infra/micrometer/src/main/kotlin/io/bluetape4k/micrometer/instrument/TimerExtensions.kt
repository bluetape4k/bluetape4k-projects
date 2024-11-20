package io.bluetape4k.micrometer.instrument

import io.micrometer.core.instrument.AbstractTimer
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.TimeUnit

suspend fun <T> Timer.recordSuspend(block: suspend () -> T): T = when (val timer = this) {
    is AbstractTimer -> timer.recordSuspendInternal(block)
    else             -> block()
}

internal suspend fun <T> AbstractTimer.recordSuspendInternal(block: suspend () -> T): T {
    val start = System.nanoTime()
    return try {
        block()
    } finally {
        val end = System.nanoTime()
        record(end - start, TimeUnit.NANOSECONDS)
    }
}

fun <T> Flow<T>.withTimer(timer: Timer): Flow<T> {
    var start = 0L
    return this
        .onStart { start = System.nanoTime() }
        .onCompletion { error ->
            if (error == null) {
                val end = System.nanoTime()
                timer.record(end - start, TimeUnit.NANOSECONDS)
            }
        }
}
