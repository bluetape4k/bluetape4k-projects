package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AbstractMemoizerTest
import io.bluetape4k.cache.memoizer.FactorialProvider
import io.bluetape4k.cache.memoizer.FibonacciProvider
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class InMemoryMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging()

    override val heavyFunc: (Int) -> Int = InMemoryMemoizer { x ->
        log.trace { "heavy($x)" }
        Thread.sleep(100)
        x * x
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long by lazy { InMemoryMemoizer { calc(it) } }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long by lazy { InMemoryMemoizer { calc(it) } }
    }

    @Test
    fun `동시 호출 시 동일 키에 대해 evaluator는 한 번만 실행된다`() {
        val evalCount = AtomicInteger(0)
        val evalStarted = CountDownLatch(1)
        val secondReady = CountDownLatch(1)
        val evalProceed = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val memo = InMemoryMemoizer<String, Int> { key ->
            evalCount.incrementAndGet()
            evalStarted.countDown()
            evalProceed.await()
            key.length
        }

        try {
            val first = executor.submit<Int> { memo("hello") }

            evalStarted.await()
            val second = executor.submit<Int> {
                secondReady.countDown()
                memo("hello")
            }

            secondReady.await()
            Thread.sleep(250)
            evalCount.get() shouldBeEqualTo 1

            evalProceed.countDown()

            first.get(10, TimeUnit.SECONDS) shouldBeEqualTo 5
            second.get(10, TimeUnit.SECONDS) shouldBeEqualTo 5
            evalCount.get() shouldBeEqualTo 1
        } finally {
            evalProceed.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `clear 중 진행 중인 동기 결과는 캐시에 저장되지 않는다`() {
        val evalCount = AtomicInteger(0)
        val evalStarted = CountDownLatch(1)
        val evalProceed = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val memo = InMemoryMemoizer<String, Int> { key ->
            evalCount.incrementAndGet()
            evalStarted.countDown()
            evalProceed.await()
            key.length
        }

        try {
            val first = executor.submit<Int> { memo("hello") }
            evalStarted.await()

            memo.clear()
            evalProceed.countDown()

            first.get(10, TimeUnit.SECONDS) shouldBeEqualTo 5
            memo("hello") shouldBeEqualTo 5
            evalCount.get() shouldBeEqualTo 2
        } finally {
            evalProceed.countDown()
            executor.shutdownNow()
        }
    }
}
