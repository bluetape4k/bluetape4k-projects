package io.bluetape4k.cache.memoizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.HazelcastServers.hazelcastClient
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds

class HazelcastSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLogging() {
        private fun <K: Any, V: Any> newMap(name: String = Base58.randomString(8)): IMap<K, V> =
            hazelcastClient.getMap<K, V>("suspend:memoizer:$name").apply { clear() }
    }

    private val heavyMap: IMap<Int, Int> = newMap("heavy")

    override val heavyFunc: suspend (Int) -> Int = heavyMap.suspendMemoizer { x ->
        delay(100)
        x * x
    }

    override val factorial = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long = newMap<Long, Long>("factorial")
            .suspendMemoizer { calc(it) }
    }

    override val fibonacci = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long = newMap<Long, Long>("fibonacci")
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
        val map: IMap<Int, Int> = newMap()
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.suspendMemoizer { key ->
            evaluateCount.incrementAndGet()
            delay(100)
            key * key
        }

        try {
            val results = List(16) { async { memoizer(7) } }.awaitAll()
            results.forEach { it shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            map.destroy()
        }
    }
}
