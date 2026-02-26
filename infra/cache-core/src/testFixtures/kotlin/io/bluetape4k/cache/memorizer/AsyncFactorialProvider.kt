package io.bluetape4k.cache.memorizer

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import java.util.concurrent.CompletableFuture

interface AsyncFactorialProvider {

    companion object: KLoggingChannel()

    val cachedCalc: (Long) -> CompletableFuture<Long>

    fun calc(x: Long): CompletableFuture<Long> {
        log.trace { "factorial($x)" }
        return when {
            x <= 1L -> CompletableFuture.completedFuture(1L)
            else -> cachedCalc(x - 1).thenApplyAsync { x * it }
        }
    }
}
