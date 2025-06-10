package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.cache.memorizer.AbstractSuspendMemorizerTest
import io.bluetape4k.cache.memorizer.SuspendFactorialProvider
import io.bluetape4k.cache.memorizer.SuspendFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.Executors

class SuspendCache2kMemorizerTest: AbstractSuspendMemorizerTest() {

    companion object: KLoggingChannel()

    private val cache = cache2k<Int, Int> {
        this.name("suspend-heavy-func")
        this.executor(Executors.newVirtualThreadPerTaskExecutor())
    }.build()

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemorizer {
        kotlinx.coroutines.delay(100L)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = cache2k<Long, Long> {
            this.name("suspend-factorial")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemorizer { calc(it) }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = cache2k<Long, Long> {
            this.name("suspend-fibonacci")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemorizer { calc(it) }
    }
}
