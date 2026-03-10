package io.bluetape4k.cache.memoizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.HazelcastServers.hazelcastClient
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class HazelcastMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging() {
        private fun <K: Any, V: Any> newMap(name: String = Base58.randomString(8)): IMap<K, V> =
            hazelcastClient.getMap<K, V>("memoizer:$name").apply { clear() }
    }

    private val heavyMap: IMap<Int, Int> = newMap("heavy")

    override val heavyFunc: (Int) -> Int = heavyMap.memoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long = newMap<Long, Long>("factorial")
            .memoizer { calc(it) }
    }

    override val fibonacci = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long = newMap<Long, Long>("fibonacci")
            .memoizer { calc(it) }
    }

    @Test
    fun `memoizer should evaluate once for same key in concurrent calls`() {
        val map: IMap<Int, Int> = newMap()
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.memoizer { key ->
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
            map.destroy()
        }
    }
}
