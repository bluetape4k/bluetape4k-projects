package io.bluetape4k.cache.memorizer

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace

interface SuspendFactorialProvider {

    companion object: KLoggingChannel()

    val cachedCalc: suspend (Long) -> Long

    suspend fun calc(n: Long): Long {
        log.trace { "factorial($n)" }
        return when {
            n <= 1L -> 1L
            else -> n * cachedCalc(n - 1)
        }
    }
}
