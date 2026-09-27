package io.bluetape4k.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

fun ExecutorService.awaitTermination(timeout: kotlin.time.Duration) =
    awaitTermination(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
