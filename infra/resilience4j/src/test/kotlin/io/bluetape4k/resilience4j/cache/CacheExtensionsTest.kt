package io.bluetape4k.resilience4j.cache

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.github.resilience4j.cache.Cache
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertFailsWith

class CacheExtensionsTest {

    companion object: KLogging()

    @Test
    fun `decoreate function1 for Cache`() {
        val jcache = CaffeineJCacheProvider.getJCache<String, String>("function1")
        val cache = Cache.of(jcache)

        var called = 0
        val function: (String) -> String = { name ->
            called++
            "Hi $name!"
        }

        // 일반 함수를 Cache로 decorate 한다
        //
        val cachedFunc = cache.decorateFunction1(function)

        cachedFunc("debop") shouldBeEqualTo "Hi debop!"
        called shouldBeEqualTo 1

        cachedFunc("Sunghyouk") shouldBeEqualTo "Hi Sunghyouk!"
        called shouldBeEqualTo 2


        cachedFunc("debop") shouldBeEqualTo "Hi debop!"
        called shouldBeEqualTo 2

        cachedFunc("Sunghyouk") shouldBeEqualTo "Hi Sunghyouk!"
        called shouldBeEqualTo 2
    }

    @Test
    fun `decorate completableFuture function for Cache`() {

        val jcache = CaffeineJCacheProvider.getJCache<String, String>("future")
        val cache = Cache.of(jcache)

        val callCount = AtomicLong(0L)
        val function: (String) -> CompletableFuture<String> = { name ->
            futureOf {
                log.trace { "Run function ... call count=${callCount.get() + 1L}" }
                Thread.sleep(100L)
                callCount.incrementAndGet()
                "Hi $name!"
            }
        }

        val cachedFunc = cache.decorateCompletableFutureFunction(function)

        cachedFunc("debop").onSuccess {
            callCount.get() shouldBeEqualTo 1L
            it shouldBeEqualTo "Hi debop!"
        }.join()

        cachedFunc("debop").onSuccess {
            callCount.get() shouldBeEqualTo 1L
            it shouldBeEqualTo "Hi debop!"
        }.join()

        cachedFunc("Sunghyouk").onSuccess {
            callCount.get() shouldBeEqualTo 2L
            it shouldBeEqualTo "Hi Sunghyouk!"
        }.join()

        cachedFunc("Sunghyouk").onSuccess {
            callCount.get() shouldBeEqualTo 2L
            it shouldBeEqualTo "Hi Sunghyouk!"
        }.join()
    }

    @Test
    fun `executeSuspendFunction 은 동일 key 동시 miss 에서 loader 를 한 번만 실행한다`() = runSuspendTest {
        val jcache = CaffeineJCacheProvider.getJCache<String, String>("suspend-concurrent")
        val cache = Cache.of(jcache)
        val callCount = AtomicInteger(0)
        val cachedLoader = cache.decorateSuspendFunction { key: String ->
            callCount.incrementAndGet()
            delay(100)
            "Hi $key!"
        }

        val results = awaitAll(
            async { cachedLoader("debop") },
            async { cachedLoader("debop") },
        )

        results[0] shouldBeEqualTo "Hi debop!"
        results[1] shouldBeEqualTo "Hi debop!"
        callCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `executeSuspendFunction 은 cache backend 예외를 miss 로 숨기지 않는다`() = runSuspendTest {
        val cache = mockk<Cache<String, String>>()
        every { cache.computeIfAbsent(any(), any()) } throws IllegalStateException("cache down")

        val error = assertFailsWith<IllegalStateException> {
            cache.executeSuspendFunction("debop") { key -> "Hi $key!" }
        }

        error.message shouldBeEqualTo "cache down"
    }
}
