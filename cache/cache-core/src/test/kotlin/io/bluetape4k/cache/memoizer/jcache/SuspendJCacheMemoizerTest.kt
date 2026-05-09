package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class SuspendJCacheMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    private val cache = JCaching.Caffeine.getOrCreate<Int, Int>("jcache-suspend-memoizer-heavy")

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemoizer {
        delay(100L.milliseconds)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = JCaching.Caffeine.getOrCreate<Long, Long>("jcache-suspend-memoizer-factorial")

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = JCaching.Caffeine.getOrCreate<Long, Long>("jcache-suspend-memoizer-fibonacci")

        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }

    @Test
    fun `clear - 살아있는 캐시 항목도 모두 제거된다`() = runSuspendDefault {
        val localCache = JCaching.Caffeine.getOrCreate<String, Int>("jcache-suspend-memoizer-clear-test")
        val memo = localCache.suspendMemoizer { key: String -> key.length }

        memo("hello") shouldBeEqualTo 5
        memo("world") shouldBeEqualTo 5

        memo.clear()

        // clear 후에는 캐시에서 직접 조회해도 null이어야 한다.
        localCache.get("hello").shouldBeNull()
        localCache.get("world").shouldBeNull()
    }
}
