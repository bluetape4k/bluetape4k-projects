package io.bluetape4k.resilience4j.cache

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.concurrent.onSuccess
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.github.resilience4j.cache.Cache
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class AbstractJCacheCoroutinesTest {

    companion object: KLoggingChannel() {
        fun randomKey(prefix: String): String = "$prefix-${UUID.randomUUID().encodeBase62()}"
    }

    abstract val jcache: javax.cache.Cache<String, String>
    private lateinit var cache: Cache<String, String>

    @BeforeEach
    open fun setup() {
        jcache.clear()
        cache = Cache.of(jcache)
    }

    private suspend fun greeting(name: String): String {
        log.debug { "Start populate. name=$name" }
        delay(10)
        log.debug { "Finish populate. name=$name" }
        return "Hi $name!"
    }

    @Test
    fun `decorate suspend function1 for Cache`() = runSuspendTest {
        cache.eventPublisher
            .onError { evt -> log.error(evt.throwable) { "Fail to get cache. $evt" } }

        val _called = atomic(0)
        val called by _called
        val function: suspend (String) -> String = { name: String ->
            _called.incrementAndGet()
            log.debug { "Cache function invoked. name=$name, called=$called" }
            greeting(name)
        }

        val cachedFunc = cache.decorateSuspendFunction(function)

        val key1 = randomKey("key")
        val key2 = randomKey("key")

        cachedFunc(key1) shouldBeEqualTo "Hi $key1!"
        called shouldBeEqualTo 1
        cachedFunc(key2) shouldBeEqualTo "Hi $key2!"
        called shouldBeEqualTo 2

        cache.metrics.numberOfCacheHits shouldBeEqualTo 0
        cache.metrics.numberOfCacheMisses shouldBeEqualTo 4

        cachedFunc(key1) shouldBeEqualTo "Hi $key1!"
        called shouldBeEqualTo 2
        cachedFunc(key2) shouldBeEqualTo "Hi $key2!"
        called shouldBeEqualTo 2

        cache.metrics.numberOfCacheHits shouldBeEqualTo 2
        cache.metrics.numberOfCacheMisses shouldBeEqualTo 4
    }

    @Test
    fun `decorate completableFuture function for Cache`() {
        val _called = atomic(0L)
        val called by _called
        val function: (String) -> CompletableFuture<String> = { name ->
            futureOf {
                log.trace { "Run function ... call count=${called + 1}" }
                Thread.sleep(100L)
                _called.incrementAndGet()
                "Hi $name!"
            }
        }

        val cachedFunc = cache.decorateCompletableFutureFunction(function)

        cachedFunc("debop").onSuccess {
            called shouldBeEqualTo 1L
            it shouldBeEqualTo "Hi debop!"
        }.join()

        cachedFunc("debop").onSuccess {
            called shouldBeEqualTo 1L
            it shouldBeEqualTo "Hi debop!"
        }.join()

        cachedFunc("Sunghyouk").onSuccess {
            called shouldBeEqualTo 2L
            it shouldBeEqualTo "Hi Sunghyouk!"
        }.join()

        cachedFunc("Sunghyouk").onSuccess {
            called shouldBeEqualTo 2L
            it shouldBeEqualTo "Hi Sunghyouk!"
        }.join()
    }

    @Test
    fun `using coroutinesCache`() = runSuspendTest {
        val coCache = SuspendCache.of(jcache)

        coCache.eventPublisher
            .onError { evt -> log.error { "Fail to get cache. $evt" } }

        val _called = atomic(0)
        val called by _called

        val loader: suspend (String) -> String = { name: String ->
            _called.incrementAndGet()
            log.debug { "Cached item... called=$called" }
            delay(1)
            greeting(name)
        }

        val cachedLoader: suspend (String) -> String = coCache.decorateSuspendFunction(loader)

        val key1 = randomKey("coKey")
        val key2 = randomKey("coKey")

        cachedLoader(key1) shouldBeEqualTo "Hi $key1!"
        called shouldBeEqualTo 1

        cachedLoader(key2) shouldBeEqualTo "Hi $key2!"
        called shouldBeEqualTo 2

        // 캐시에 새로 등록하므로 CacheHits 는 0 이다.
        coCache.metrics.getNumberOfCacheHits() shouldBeEqualTo 0
        coCache.metrics.getNumberOfCacheMisses() shouldBeEqualTo 2

        cachedLoader(key1) shouldBeEqualTo "Hi $key1!"
        called shouldBeEqualTo 2
        cachedLoader(key2) shouldBeEqualTo "Hi $key2!"
        called shouldBeEqualTo 2

        // 캐시에 두 key가 등록되었으므로 CacheHits 는 2가 된다
        coCache.metrics.getNumberOfCacheHits() shouldBeEqualTo 2
        coCache.metrics.getNumberOfCacheMisses() shouldBeEqualTo 2
    }
}
