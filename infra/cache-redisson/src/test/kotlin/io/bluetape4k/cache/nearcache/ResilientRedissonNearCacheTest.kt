package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.faker.Fakers

/**
 * [RedissonNearCache] + [ResilientNearCacheDecorator] 통합 테스트.
 *
 * [AbstractResilientNearCacheOperationsTest]를 상속하여 CRUD + 동시성 + Resilience를 모두 검증합니다.
 */
class ResilientRedissonNearCacheTest : AbstractResilientNearCacheOperationsTest<String>() {
    private val cacheName get() = "resilient-redisson-test-${Fakers.randomString(6, 8)}"

    override fun createBaseCache(): NearCacheOperations<String> =
        RedissonNearCache(
            redisson = RedisServers.redisson,
            config = RedissonNearCacheConfig(cacheName = cacheName),
        )

    override fun sampleValue(): String = Fakers.randomString(8, 32)
    override fun anotherValue(): String = Fakers.randomString(8, 32)
}
