package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
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
}
