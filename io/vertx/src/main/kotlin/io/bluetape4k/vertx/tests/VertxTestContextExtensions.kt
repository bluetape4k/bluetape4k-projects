package io.bluetape4k.vertx.tests

import io.vertx.junit5.VertxTestContext
import java.util.concurrent.TimeUnit

fun VertxTestContext.awaitCompletion(timeout: kotlin.time.Duration): Boolean =
    awaitCompletion(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
