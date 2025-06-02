package io.bluetape4k.cache.memorizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace

interface FibonacciProvider {

    companion object: KLogging()

    val cachedCalc: (Long) -> Long

    fun calc(n: Long): Long {
        log.trace { "fibonacci($n)" }
        return when {
            n <= 0L -> 0L
            n <= 2L -> 1L
            else    -> cachedCalc(n - 1) + cachedCalc(n - 2)
        }
    }
}
