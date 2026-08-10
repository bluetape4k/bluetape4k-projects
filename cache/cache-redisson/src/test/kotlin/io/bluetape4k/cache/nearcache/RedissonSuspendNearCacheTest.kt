package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.faker.Fakers
import org.junit.jupiter.api.Test

/**
 * [RedissonSuspendNearCache] 테스트.
 *
 * Redisson [org.redisson.api.RLocalCachedMap] 기반 [SuspendNearCacheOperations] 구현체를 검증합니다.
 */
class RedissonSuspendNearCacheTest: AbstractSuspendNearCacheOperationsTest<String>() {
    private val cacheName get() = "redisson-suspend-near-cache-test-${Fakers.randomString(6, 8)}"

    override fun createCache(): SuspendNearCacheOperations<String> =
        RedissonSuspendNearCache(
            redisson = RedisServers.redisson,
            config = RedissonNearCacheConfig(cacheName = cacheName)
        )

    override fun sampleValue(): String = Fakers.randomString(8, 32)

    override fun anotherValue(): String = Fakers.randomString(8, 32)

    @Test
    fun `back cache size and statistics expose Redis state`() = runSuspendIO {
        val cache = RedissonSuspendNearCache<String>(
            redisson = RedisServers.redisson,
            config = RedissonNearCacheConfig(cacheName = cacheName)
        )
        try {
            cache.put("size-key", "size-value")
            cache.backCacheSize() shouldBeEqualTo 1L
            cache.get("size-key") shouldBeEqualTo "size-value"
            cache.get("missing-key")
            cache.stats().backHits shouldBeEqualTo 1L
            cache.stats().backMisses shouldBeEqualTo 1L

            cache.removeAll(emptySet())
            cache.clearAll()
            cache.backCacheSize() shouldBeEqualTo 0L
            cache.close()
            cache.isClosed.shouldBeTrue()
        } finally {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `factory creates a suspend near cache with the requested name`() = runSuspendIO {
        val name = cacheName
        val cache = redissonSuspendNearCacheOf<String>(
            redisson = RedisServers.redisson,
            config = RedissonNearCacheConfig(cacheName = name)
        )
        try {
            cache.cacheName shouldBeEqualTo name
            cache.put("factory-key", "factory-value")
            cache.get("factory-key") shouldBeEqualTo "factory-value"
        } finally {
            runCatching { cache.close() }
        }
    }
}
