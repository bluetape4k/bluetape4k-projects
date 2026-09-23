package io.bluetape4k.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun CountDownLatch.await(duration: Duration): Boolean =
    await(duration.inWholeNanoseconds, TimeUnit.NANOSECONDS)
