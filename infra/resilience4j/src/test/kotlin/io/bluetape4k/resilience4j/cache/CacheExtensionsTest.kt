package io.bluetape4k.resilience4j.cache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
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
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

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
            delay(100.milliseconds)
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

    @Test
    fun `executeSuspendFunction propagates cache cancellation`() = runSuspendTest {
        val cancellation = CancellationException("cache cancelled")
        val cache = mockk<Cache<String, String>>()
        every { cache.computeIfAbsent(any(), any()) } throws cancellation

        val thrown = assertFailsWith<CancellationException> {
            cache.executeSuspendFunction("debop") { key -> "Hi $key!" }
        }

        thrown.message shouldBeEqualTo cancellation.message
    }

    @Test
    fun `executeSuspendFunction propagates loader cancellation`() = runSuspendTest {
        val jcache = CaffeineJCacheProvider.getJCache<String, String>("loader-cancel")
        jcache.clear()
        val cache = Cache.of(jcache)
        val cancellation = CancellationException("loader cancelled")

        val thrown = assertFailsWith<CancellationException> {
            cache.executeSuspendFunction("debop") {
                throw cancellation
            }
        }

        thrown.message shouldBeEqualTo cancellation.message
    }

    @Test
    fun `CacheCoroutineLocks release removes mutex entry when all callers release`() = runSuspendTest {
        val jcache = CaffeineJCacheProvider.getJCache<String, String>("refcount-test")
        val cache = Cache.of(jcache)
        val key = "refcount-test-key"

        // Two callers acquire a Mutex for the same key — they must get the same instance.
        val mutex1 = CacheCoroutineLocks.mutexFor(cache, key)
        val mutex2 = CacheCoroutineLocks.mutexFor(cache, key)
        (mutex1 === mutex2) shouldBeEqualTo true

        // Release one of the two references — entry must still be alive.
        CacheCoroutineLocks.release(cache, key, mutex1)
        val mutex3 = CacheCoroutineLocks.mutexFor(cache, key)
        (mutex3 === mutex1) shouldBeEqualTo true   // same entry still present

        // Release all remaining references (mutex2 + mutex3 = 2 remaining).
        CacheCoroutineLocks.release(cache, key, mutex2)
        CacheCoroutineLocks.release(cache, key, mutex3)

        // Entry has been removed. A fresh mutexFor call must create a new Mutex instance.
        val mutex4 = CacheCoroutineLocks.mutexFor(cache, key)
        (mutex4 === mutex1) shouldBeEqualTo false  // new Mutex after full release
        CacheCoroutineLocks.release(cache, key, mutex4)
    }
}
