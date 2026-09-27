package io.bluetape4k.concurrent

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

fun <V> Future<V>.get(timeout: kotlin.time.Duration): V =
    get(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
