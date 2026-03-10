package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers.randomName
import io.bluetape4k.cache.RedisServers.redisson
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RedissonMemoizerTest: AbstractMemoizerTest() {

    private val heavyMap = redisson
        .getMap<Int, Int>("memoizer:heavy", IntegerCodec())
        .apply { clear() }

    override val heavyFunc: (Int) -> Int = heavyMap.memoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long = redisson
            .getMap<Long, Long>("memoizer:factorial", LongCodec())
            .memoizer { calc(it) }
    }

    override val fibonacci = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long = redisson
            .getMap<Long, Long>("memoizer:fibonacci", LongCodec())
            .memoizer { calc(it) }
    }

    @Test
    fun `memoizer should evaluate once for same key in concurrent calls`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
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
            map.delete()
        }
    }

}
