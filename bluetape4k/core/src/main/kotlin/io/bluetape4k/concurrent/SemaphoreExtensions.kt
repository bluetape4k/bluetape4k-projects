package io.bluetape4k.concurrent

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

fun Semaphore.tryAcquire(timeout: kotlin.time.Duration): Boolean =
    tryAcquire(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)

fun Semaphore.tryAcquire(timeout: java.time.Duration): Boolean =
    tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS)
