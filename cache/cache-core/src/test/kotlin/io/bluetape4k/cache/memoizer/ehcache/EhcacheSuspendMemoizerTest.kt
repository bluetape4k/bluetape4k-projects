package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.ehcache.ehcacheManager
import io.bluetape4k.cache.ehcache.getOrCreateCache
import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class EhcacheSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    private val ehcacheManager = ehcacheManager { }
    private val cache = ehcacheManager.getOrCreateCache<Int, Int>("suspend-heavy")

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemoizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = ehcacheManager.getOrCreateCache<Long, Long>("suspend-factorial")
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }
    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = ehcacheManager.getOrCreateCache<Long, Long>("suspend-fibonacci")
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }

    @Test
    fun `evaluator 실패 후 같은 키를 다시 호출하면 새 계산으로 복구된다`() = runSuspendDefault {
        val localCache = ehcacheManager.getOrCreateCache<String, Int>("suspend-recovery-${System.nanoTime()}")
        val evalCount = AtomicInteger(0)
        val memo = localCache.suspendMemoizer { key: String ->
            if (evalCount.incrementAndGet() == 1) {
                error("transient failure")
            }
            key.length
        }

        assertFailsWith<IllegalStateException> {
            memo("recover")
        }

        memo("recover") shouldBeEqualTo 7
        evalCount.get() shouldBeEqualTo 2
    }
}
