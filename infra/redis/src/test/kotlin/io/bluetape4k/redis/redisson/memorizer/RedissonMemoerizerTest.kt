package io.bluetape4k.redis.redisson.memorizer

import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.redisson.api.RedissonClient
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

    private val factorial by lazy { RedissonFactorialProvider(redisson) }
    private val fibonacci by lazy { RedissonFibonacciProvider(redisson) }

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
}

abstract class FactorialProvider {

    companion object: KLogging()

    abstract val cachedCalc: (kotlin.Long) -> kotlin.Long

    fun calc(n: Long): Long {
        log.trace { "factorial($n)" }
        return when {
            n <= 1L -> 1L
            else    -> n * cachedCalc(n - 1)
        }
    }
}


class RedissonFactorialProvider(redisson: RedissonClient): FactorialProvider() {

    private val map = redisson
        .getMap<Long, Long>("memorizer:factorial", LongCodec())
        .apply { clear() }

    override val cachedCalc: (Long) -> Long = map.memorizer { calc(it) }
}

abstract class FibonacciProvider {

    companion object: KLogging()

    abstract val cachedCalc: (Long) -> Long

    fun calc(n: Long): Long {
        log.trace { "fibonacci($n)" }
        return when {
            n <= 0L -> 0L
            n <= 2L -> 1L
            else    -> cachedCalc(n - 1) + cachedCalc(n - 2)
        }
    }
}

class RedissonFibonacciProvider(redisson: RedissonClient): FibonacciProvider() {

    private val map = redisson
        .getMap<Long, Long>("memorizer:fibonacci", LongCodec())
        .apply { clear() }

    override val cachedCalc: Memorizer<Long, Long> = map.memorizer { calc(it) }
}
