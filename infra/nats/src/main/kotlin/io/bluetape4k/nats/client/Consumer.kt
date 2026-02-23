package io.bluetape4k.nats.client

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.nats.client.Consumer
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.time.toJavaDuration

fun Consumer.drain(timeoutMillis: Long): CompletableFuture<Boolean> {
    timeoutMillis.requireZeroOrPositiveNumber("timeoutMillis")
    return drain(Duration.ofMillis(timeoutMillis))
}

fun Consumer.drain(timeout: kotlin.time.Duration): CompletableFuture<Boolean> {
    timeout.requireGe(kotlin.time.Duration.ZERO, "timeout")
    return drain(timeout.toJavaDuration())
}

suspend fun Consumer.drainSuspending(timeoutMillis: Long): Boolean {
    return drain(timeoutMillis).await()
}

suspend fun Consumer.drainSuspending(timeout: kotlin.time.Duration): Boolean {
    return drain(timeout).await()
}

suspend fun Consumer.drainSuspending(timeout: java.time.Duration): Boolean {
    timeout.requireGe(java.time.Duration.ZERO, "timeout")
    return drain(timeout).await()
}
