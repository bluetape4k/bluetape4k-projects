package io.bluetape4k.cache.memorizer

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace

interface SuspendFibonacciProvider {

    companion object: KLoggingChannel()

    val cachedCalc: suspend (Long) -> Long

    suspend fun calc(n: Long): Long {
        log.trace { "suspend fibonacci($n)" }

        return when {
            n <= 0L -> 0L
            n <= 2L -> 1L
            else -> cachedCalc(n - 1) + cachedCalc(n - 2)
        }
    }
}
