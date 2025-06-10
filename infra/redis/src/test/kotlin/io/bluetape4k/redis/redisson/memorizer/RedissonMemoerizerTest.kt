package io.bluetape4k.redis.redisson.memorizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import java.time.Duration
import kotlin.system.measureTimeMillis

class RedissonMemoerizerTest: AbstractRedissonTest() {

    private val heavyMap = redisson
        .getMap<Int, Int>("memorizer:heavy", IntegerCodec())
        .apply { clear() }

    val heavyFunc: (Int) -> Int = heavyMap.memorizer { x ->
        Thread.sleep(100)
        x * x
    }

    private val factorial = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long = redisson
            .getMap<Long, Long>("memorizer:factorial", LongCodec())
            .memorizer { calc(it) }
    }

    private val fibonacci = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long = redisson
            .getMap<Long, Long>("memorizer:fibonacci", LongCodec())
            .memorizer { calc(it) }
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
