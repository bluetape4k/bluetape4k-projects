package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.faker.Fakers

/**
 * [RedissonSuspendNearCache] + [ResilientSuspendNearCacheDecorator] 통합 테스트.
 *
 * [AbstractResilientSuspendNearCacheOperationsTest]를 상속하여 CRUD + 동시성 + Resilience를 모두 검증합니다.
 */
class ResilientRedissonSuspendNearCacheTest: AbstractResilientSuspendNearCacheOperationsTest<String>() {
    private val cacheName get() = "resilient-redisson-suspend-test-${Fakers.randomString(6, 8)}"

    override fun createBaseCache(): SuspendNearCacheOperations<String> =
        RedissonSuspendNearCache(
            redisson = RedisServers.redisson,
            config = RedissonNearCacheConfig(cacheName = cacheName),
        )

    override fun sampleValue(): String = Fakers.randomString(8, 32)
    override fun anotherValue(): String = Fakers.randomString(8, 32)
}
