package io.bluetape4k.cache.memorizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memorizer.AbstractSuspendMemorizerTest
import io.bluetape4k.cache.memorizer.SuspendFactorialProvider
import io.bluetape4k.cache.memorizer.SuspendFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

class SuspendCaffeineMemorizerTest: AbstractSuspendMemorizerTest() {

    companion object: KLoggingChannel()

    private val caffeine: Caffeine<Any, Any> = caffeine {
        executor(Executors.newVirtualThreadPerTaskExecutor())
    }
    private val cache: Cache<Int, Int> = caffeine.cache()

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemorizer {
        delay(100L)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = caffeine.cache<Long, Long>()
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemorizer { calc(it) }
    }
    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = caffeine.cache<Long, Long>()
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemorizer { calc(it) }
    }
}
