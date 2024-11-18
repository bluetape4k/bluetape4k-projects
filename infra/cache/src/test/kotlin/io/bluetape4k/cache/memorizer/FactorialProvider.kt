package io.bluetape4k.cache.memorizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace

abstract class FactorialProvider {

    companion object: KLogging()

    abstract val cachedCalc: (Long) -> Long

    fun calc(n: Long): Long {
        log.trace { "factorial($n)" }
        return when {
            n <= 1L -> 1L
            else    -> n * cachedCalc(n - 1)
        }
    }
}
