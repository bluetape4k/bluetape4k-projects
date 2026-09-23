package io.bluetape4k.concurrent

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun CyclicBarrier.await(timeout: Duration): Int =
    await(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
