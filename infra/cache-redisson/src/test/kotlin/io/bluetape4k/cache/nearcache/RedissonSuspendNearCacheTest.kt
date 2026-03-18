package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.faker.Fakers

/**
 * [RedissonSuspendNearCache] 테스트.
 *
 * Redisson [org.redisson.api.RLocalCachedMap] 기반 [SuspendNearCacheOperations] 구현체를 검증합니다.
 */
class RedissonSuspendNearCacheTest : AbstractSuspendNearCacheOperationsTest<String>() {
    private val cacheName get() = "redisson-suspend-near-cache-test-${Fakers.randomString(6, 8)}"

    override fun createCache(): SuspendNearCacheOperations<String> =
        RedissonSuspendNearCache(
            redisson = RedisServers.redisson,
            config = RedissonNearCacheConfig(cacheName = cacheName)
        )

    override fun sampleValue(): String = Fakers.randomString(8, 32)

    override fun anotherValue(): String = Fakers.randomString(8, 32)
}
