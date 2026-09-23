package io.bluetape4k.concurrent

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun <T> Future<T>.get(timeout: Duration): T =
    get(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
