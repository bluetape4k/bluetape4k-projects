package io.bluetape4k.resilience4j.cache

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.github.resilience4j.cache.Cache
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

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
}
