package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.IgniteServers
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.client.ClientCache
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds

class IgniteSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLogging() {
        private val igniteClient by lazy { IgniteServers.igniteClient }

        private fun <K: Any, V: Any> newCache(name: String = Base58.randomString(8)): ClientCache<K, V> =
            igniteClient.getOrCreateCache<K, V>("suspend:memoizer:$name").apply { clear() }
    }

    private val heavyCache: ClientCache<Int, Int> = newCache("heavy")

    override val heavyFunc: suspend (Int) -> Int = heavyCache.suspendMemoizer { x ->
        delay(100)
        x * x
    }

    override val factorial = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long = newCache<Long, Long>("factorial")
            .suspendMemoizer { calc(it) }
    }

    override val fibonacci = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long = newCache<Long, Long>("fibonacci")
            .suspendMemoizer { calc(it) }
    }

    @Test
    fun `run heavy function`() = runSuspendIO {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        val result = withTimeoutOrNull(1.seconds) {
            heavyFunc(10)
        }
        result shouldBeEqualTo 100
    }

    @Test
    fun `run factorial`() = runSuspendIO {
        val x1 = factorial.calc(100)

        val result = withTimeoutOrNull(3.seconds) {
            factorial.calc(100)
        }
        result shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() = runSuspendIO {
        val x1 = fibonacci.calc(100)

        val result = withTimeoutOrNull(3.seconds) {
            fibonacci.calc(100)
        }
        result shouldBeEqualTo x1
    }

    @Test
    fun `suspend memoizer should evaluate once for same key in concurrent calls`() = runSuspendIO {
        val cache: ClientCache<Int, Int> = newCache()
        val evaluateCount = AtomicInteger(0)
        val memoizer = cache.suspendMemoizer { key ->
            evaluateCount.incrementAndGet()
            delay(100)
            key * key
        }

        try {
            val results = List(16) { async { memoizer(7) } }.awaitAll()
            results.forEach { it shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            cache.clear()
        }
    }
}
