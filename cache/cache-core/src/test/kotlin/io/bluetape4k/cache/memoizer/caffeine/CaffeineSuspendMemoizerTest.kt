package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class CaffeineSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    private val caffeine: Caffeine<Any, Any> = caffeine {
        executor(Executors.newVirtualThreadPerTaskExecutor())
    }
    private val cache: Cache<Int, Int> = caffeine.cache()

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemoizer {
        delay(100L.milliseconds)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = caffeine.cache<Long, Long>()
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }
    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = caffeine.cache<Long, Long>()
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }

    /**
     * clear()는 cleanUp()이 아닌 invalidateAll()을 호출해야 한다.
     * cleanUp()은 만료된 항목만 제거하므로 아직 살아있는 항목은 남아 있게 된다.
     */
    @Test
    fun `clear - 살아있는 캐시 항목도 모두 제거된다`() = runSuspendDefault {
        val localCache = caffeine.cache<String, Int>()
        val memo = localCache.suspendMemoizer { key: String -> key.length }

        memo("hello") shouldBeEqualTo 5
        memo("world") shouldBeEqualTo 5

        memo.clear()

        // clear 후에는 캐시에서 직접 조회해도 null이어야 한다.
        localCache.getIfPresent("hello").shouldBeNull()
        localCache.getIfPresent("world").shouldBeNull()
    }

    @Test
    fun `evaluator 실패 후 같은 키를 다시 호출하면 새 계산으로 복구된다`() = runSuspendDefault {
        val localCache = caffeine.cache<String, Int>()
        val evalCount = AtomicInteger(0)
        val memo = localCache.suspendMemoizer { key: String ->
            if (evalCount.incrementAndGet() == 1) {
                error("transient failure")
            }
            key.length
        }

        assertFailsWith<IllegalStateException> {
            memo("recover")
        }

        // 실패한 in-flight Deferred가 제거되어야 이후 동시 호출들이 새 계산을 공유한다.
        SuspendedJobTester()
            .workers(8)
            .rounds(2)
            .add {
                memo("recover") shouldBeEqualTo 7
            }
            .run()

        memo("recover") shouldBeEqualTo 7
        localCache.getIfPresent("recover") shouldBeEqualTo 7

        // Caffeine의 executor 스케줄링에 따라 복구 계산 직후 일부 경합이 생길 수 있다.
        // 핵심 계약은 실패한 Deferred를 재사용하지 않고 성공 값을 다시 캐시하는 것이다.
        evalCount.get() shouldBeLessOrEqualTo 3
    }
}
