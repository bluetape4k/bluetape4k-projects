package io.bluetape4k.cache.memorizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.util.concurrent.CompletableFuture

abstract class AsyncFactorialProvider {

    companion object: KLogging()

    abstract val cachedCalc: (Long) -> CompletableFuture<Long>

    fun calc(x: Long): CompletableFuture<Long> {
        log.trace { "factorial($x)" }
        return when {
            x <= 1L -> CompletableFuture.completedFuture(1L)
            else    -> cachedCalc(x - 1).thenApplyAsync { x * it }
        }
    }
}
