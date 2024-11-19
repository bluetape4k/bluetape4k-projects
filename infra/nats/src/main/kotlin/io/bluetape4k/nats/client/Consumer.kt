package io.bluetape4k.nats.client

import io.nats.client.Consumer
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.time.toJavaDuration

fun Consumer.drain(timeoutMillis: Long): CompletableFuture<Boolean> =
    drain(Duration.ofMillis(timeoutMillis))

fun Consumer.drain(timeout: kotlin.time.Duration): CompletableFuture<Boolean> =
    drain(timeout.toJavaDuration())
