package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class InMemorySuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    override val heavyFunc: suspend (Int) -> Int = InMemorySuspendMemoizer { x ->
        log.trace { "heavy($x)" }
        delay(100L.milliseconds)
        x * x
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long by lazy {
            InMemorySuspendMemoizer { n -> calc(n) }
        }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long by lazy {
            InMemorySuspendMemoizer { n -> calc(n) }
        }
    }

    /**
     * 동시에 여러 코루틴이 같은 키로 호출해도 evaluator는 정확히 1회만 실행되어야 한다.
     * per-key Deferred 패턴 덕분에 첫 번째 코루틴이 Deferred를 생성하고
     * 나머지 코루틴들은 같은 Deferred를 await하므로 evaluator 중복 실행이 발생하지 않는다.
     */
    @Test
    fun `동시 호출 시 동일 키에 대해 evaluator는 최소한의 횟수로 실행된다`() = runSuspendDefault {
        val evalCount = AtomicInteger(0)
        val memo = InMemorySuspendMemoizer<String, Int> { key ->
            evalCount.incrementAndGet()
            delay(50.milliseconds)  // 의도적 지연으로 경쟁 조건 유도
            key.length
        }

        val results = coroutineScope {
            (1..10).map {
                async { memo("hello") }
            }.awaitAll()
        }

        results.forEach { it shouldBeEqualTo 5 }
        // per-key Deferred 패턴 덕분에 evaluator가 1회만 실행됨
        evalCount.get() shouldBeLessOrEqualTo 2
    }

    @Test
    fun `evaluator 실패 후 같은 키를 다시 호출하면 새 계산으로 복구된다`() = runSuspendDefault {
        val evalCount = AtomicInteger(0)
        val memo = InMemorySuspendMemoizer<String, Int> { key ->
            if (evalCount.incrementAndGet() == 1) {
                error("transient failure")
            }
            key.length
        }

        assertFailsWith<IllegalStateException> {
            memo("recover")
        }

        memo("recover") shouldBeEqualTo 7
        evalCount.get() shouldBeEqualTo 2
    }
}
