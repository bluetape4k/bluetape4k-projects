package io.bluetape4k.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun CountDownLatch.await(duration: kotlin.time.Duration): Boolean =
    await(duration.inWholeNanoseconds, TimeUnit.NANOSECONDS)


fun CountDownLatch.await(duration: java.time.Duration): Boolean =
    await(duration.toNanos(), TimeUnit.NANOSECONDS)
