package io.bluetape4k.concurrent

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

fun CyclicBarrier.await(timeout: kotlin.time.Duration): Int =
    await(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
