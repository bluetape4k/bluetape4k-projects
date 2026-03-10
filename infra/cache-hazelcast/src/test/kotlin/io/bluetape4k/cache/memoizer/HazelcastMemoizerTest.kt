package io.bluetape4k.cache.memoizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.memoizer.hazelcast.memoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.testcontainers.utility.Base58
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class HazelcastMemoizerTest {

    companion object: KLogging() {
        private val hazelcastClient by lazy { HazelcastServers.hazelcastClient }

        private fun <K: Any, V: Any> newMap(name: String = Base58.randomString(8)): IMap<K, V> =
            hazelcastClient.getMap<K, V>("memoizer:$name").apply { clear() }
    }

    private val heavyMap: IMap<Int, Int> = newMap("heavy")

    val heavyFunc: (Int) -> Int = heavyMap.memoizer { x ->
        Thread.sleep(100)
        x * x
    }

    private val factorial = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long = newMap<Long, Long>("factorial")
            .memoizer { calc(it) }
    }

    private val fibonacci = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long = newMap<Long, Long>("fibonacci")
            .memoizer { calc(it) }
    }

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        assertTimeout(Duration.ofMillis(1000)) {
            heavyFunc(10) shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(100)

        assertTimeout(Duration.ofMillis(1000)) {
            factorial.calc(100)
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(100)

        assertTimeout(Duration.ofMillis(1000)) {
            fibonacci.calc(100)
        } shouldBeEqualTo x1
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

    interface FactorialProvider {
        companion object: KLogging()

        val cachedCalc: (Long) -> Long

        fun calc(n: Long): Long {
            log.trace { "factorial($n)" }
            return when {
                n <= 1L -> 1L
                else -> n * cachedCalc(n - 1)
            }
        }
    }

    interface FibonacciProvider {
        companion object: KLogging()

        val cachedCalc: (Long) -> Long

        fun calc(n: Long): Long {
            log.trace { "fibonacci($n)" }
            return when {
                n <= 0L -> 0L
                n <= 2L -> 1L
                else -> cachedCalc(n - 1) + cachedCalc(n - 2)
            }
        }
    }
}
