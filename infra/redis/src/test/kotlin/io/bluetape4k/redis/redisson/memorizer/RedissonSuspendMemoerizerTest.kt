package io.bluetape4k.redis.redisson.memorizer

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.atomic.AtomicInteger

class RedissonSuspendMemoerizerTest: AbstractRedissonTest() {

    private val heavyMap = redisson
        .getMap<Int, Int>("suspend:memorizer:heavy", IntegerCodec())
        .apply { clear() }

    val heavyFunc: suspend (Int) -> Int = heavyMap.suspendMemorizer { x ->
        Thread.sleep(100)
        x * x
    }

    private val factorial = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long = redisson
            .getMap<Long, Long>("suspend:memorizer:factorial", LongCodec())
            .suspendMemorizer { calc(it) }
    }
    private val fibonacci = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long = redisson
            .getMap<Long, Long>("suspend:memorizer:fibonacci", LongCodec())
            .suspendMemorizer { calc(it) }
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
    fun `suspend memorizer should evaluate once for same key in concurrent calls`() = runSuspendIO {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memorizer = map.suspendMemorizer { key ->
            evaluateCount.incrementAndGet()
            delay(100)
            key * key
        }

        try {
            val results = List(16) { async { memorizer(7) } }.awaitAll()
            results.forEach { it shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            map.delete()
        }
    }

    interface SuspendFactorialProvider {

        companion object: KLogging()

        val cachedCalc: suspend (Long) -> Long

        suspend fun calc(n: Long): Long {
            log.trace { "factorial($n)" }
            return when {
                n <= 1L -> 1L
                else -> n * cachedCalc(n - 1)
            }
        }
    }


    interface SuspendFibonacciProvider {

        companion object: KLogging()

        val cachedCalc: suspend (Long) -> Long

        suspend fun calc(n: Long): Long {
            log.trace { "fibonacci($n)" }
            return when {
                n <= 0L -> 0L
                n <= 2L -> 1L
                else -> cachedCalc(n - 1) + cachedCalc(n - 2)
            }
        }
    }
}
