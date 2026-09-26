package io.bluetape4k.temporal

import io.temporal.worker.WorkerFactory
import java.util.concurrent.TimeUnit

fun WorkerFactory.awaitTermination(timeout: kotlin.time.Duration) {
    awaitTermination(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
}
