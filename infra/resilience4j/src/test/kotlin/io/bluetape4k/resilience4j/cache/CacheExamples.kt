package io.bluetape4k.resilience4j.cache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.github.resilience4j.cache.Cache
import io.github.resilience4j.decorators.Decorators
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class CacheExamples {

    companion object: KLogging()

    private val jcache: javax.cache.Cache<String, String> by lazy {
        JCaching.Caffeine.getOrCreate("jcache-" + UUID.randomUUID().encodeBase62())
    }
    private val resilienceCache: Cache<String, String> = Cache.of(jcache)

    @BeforeEach
    fun setup() {
        jcache.clear()
    }

    @Test
    fun `setup resilience4j cache with caffein jcache`() {
        resilienceCache.eventPublisher.onEvent { log.debug { "onEvent=$it" } }
        resilienceCache.eventPublisher.onError { log.warn(it.throwable) { "OnError. FlowEvent=${it}" } }

        val called = AtomicInteger(0)

        val function: () -> String = {
            called.incrementAndGet()
            "Do something"
        }

        val cachedFunction = Decorators
            .ofSupplier(function)
            .withCache(resilienceCache)
            .decorate()

        cachedFunction.apply("cacheKey") shouldBeEqualTo "Do something"
        cachedFunction.apply("cacheKey") shouldBeEqualTo "Do something"

        called.get() shouldBeEqualTo 1

        resilienceCache.metrics.numberOfCacheHits shouldBeEqualTo 1
        resilienceCache.metrics.numberOfCacheMisses shouldBeEqualTo 1
    }
}
