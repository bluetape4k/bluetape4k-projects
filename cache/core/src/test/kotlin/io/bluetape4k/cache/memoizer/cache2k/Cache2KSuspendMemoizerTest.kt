package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import org.cache2k.Cache
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

class Cache2KSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    private val cache: Cache<Int, Int> = cache2k<Int, Int> {
        this.name("suspend-heavy-func")
        this.executor(Executors.newVirtualThreadPerTaskExecutor())
    }.build()

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemoizer {
        delay(100L.milliseconds)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = cache2k<Long, Long> {
            this.name("suspend-factorial")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = cache2k<Long, Long> {
            this.name("suspend-fibonacci")
            this.executor(Executors.newVirtualThreadPerTaskExecutor())
        }.build()

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }
}
