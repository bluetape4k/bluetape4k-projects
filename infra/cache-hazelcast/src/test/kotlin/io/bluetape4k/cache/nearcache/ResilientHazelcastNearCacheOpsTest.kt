package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.junit5.faker.Fakers

/**
 * [HazelcastNearCache] + [ResilientNearCacheDecorator] 통합 테스트.
 *
 * [AbstractResilientNearCacheOperationsTest]를 상속하여 CRUD + 동시성 + Resilience를 모두 검증합니다.
 */
class ResilientHazelcastNearCacheOpsTest : AbstractResilientNearCacheOperationsTest<String>() {
    private val cacheName get() = "resilient-hazelcast-test-${Fakers.randomString(6, 8)}"

    override fun createBaseCache(): NearCacheOperations<String> =
        HazelcastNearCache(
            hazelcastInstance = HazelcastServers.hazelcastClient,
            config = HazelcastNearCacheConfig(cacheName = cacheName),
        )

    override fun sampleValue(): String = Fakers.randomString(8, 32)
    override fun anotherValue(): String = Fakers.randomString(8, 32)
}
