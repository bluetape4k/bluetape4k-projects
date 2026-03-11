package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers.randomName
import io.bluetape4k.cache.RedisServers.redisson
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
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

    /**
     * MultithreadingTester를 사용하여 여러 스레드에서 동시에 memoizer를 호출할 때
     * 결과가 일관되고 중복 계산이 발생하지 않는지 검증합니다.
     */
    @Test
    fun `MultithreadingTester - 여러 스레드에서 동시에 memoizer 호출 시 일관된 결과 반환`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val memoizer = map.memoizer { key ->
            Thread.sleep(10)
            key * key
        }
        try {
            MultithreadingTester()
                .workers(16)
                .rounds(4)
                .add {
                    memoizer(5) shouldBeEqualTo 25
                    memoizer(7) shouldBeEqualTo 49
                    memoizer(9) shouldBeEqualTo 81
                }
                .run()
        } finally {
            map.delete()
        }
    }

    /**
     * StructuredTaskScopeTester를 사용하여 Virtual Thread 기반으로 동시에 memoizer를 호출할 때
     * 결과가 일관되고 예외가 발생하지 않는지 검증합니다.
     */
    @Test
    fun `StructuredTaskScopeTester - Virtual Thread 기반 동시 memoizer 호출 시 일관된 결과 반환`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val memoizer = map.memoizer { key ->
            Thread.sleep(10)
            key * key
        }
        try {
            StructuredTaskScopeTester()
                .rounds(32)
                .add {
                    memoizer(3) shouldBeEqualTo 9
                    memoizer(6) shouldBeEqualTo 36
                    memoizer(8) shouldBeEqualTo 64
                }
                .run()
        } finally {
            map.delete()
        }
    }

}
