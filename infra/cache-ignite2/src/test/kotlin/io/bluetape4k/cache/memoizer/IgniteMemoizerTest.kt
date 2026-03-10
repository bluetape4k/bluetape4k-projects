package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.IgniteServers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.client.ClientCache
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class IgniteMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging() {
        private val igniteClient by lazy { IgniteServers.igniteClient }

        private fun <K: Any, V: Any> newCache(name: String = Base58.randomString(8)): ClientCache<K, V> =
            igniteClient.getOrCreateCache<K, V>("memoizer:$name").apply { clear() }
    }

    private val heavyCache: ClientCache<Int, Int> = newCache("heavy")

    override val heavyFunc: (Int) -> Int = heavyCache.memoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long = newCache<Long, Long>("factorial")
            .memoizer { calc(it) }
    }

    override val fibonacci = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long = newCache<Long, Long>("fibonacci")
            .memoizer { calc(it) }
    }

    @Test
    fun `memoizer should evaluate once for same key in concurrent calls`() {
        val cache: ClientCache<Int, Int> = newCache()
        val evaluateCount = AtomicInteger(0)
        val memoizer = cache.memoizer { key ->
            evaluateCount.incrementAndGet()
            Thread.sleep(100)
            key * key
        }
        val pool = Executors.newFixedThreadPool(16)
        val startLatch = CountDownLatch(1)

        try {
            val tasks = List(16) {
                pool.submit<Int> {
                    startLatch.await(1, TimeUnit.SECONDS)
                    memoizer(7)
                }
            }
            startLatch.countDown()
            tasks.forEach { it.get(2, TimeUnit.SECONDS) shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            pool.shutdownNow()
            cache.clear()
        }
    }

}
