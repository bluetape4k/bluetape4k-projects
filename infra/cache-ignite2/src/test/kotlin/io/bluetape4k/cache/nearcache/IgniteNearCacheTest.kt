package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.IgniteJCaching
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Order
import java.util.*

// Order(2): IgniteCachingProvider 로 embedded Ignite 노드를 시작하므로, thin client 를 사용하는
// IgniteSuspendNearCacheTest(Order 1) 이후에 실행되어야 간섭이 발생하지 않습니다.
@Order(2)
class IgniteNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging()

    override val nearCacheCfg1 = NearCacheConfig<String, Any>(checkExpiryPeriod = 1_000)
    override val nearCacheCfg2 = NearCacheConfig<String, Any>(checkExpiryPeriod = 1_000)

    override val backCache: JCache<String, Any> by lazy {
        IgniteJCaching.getOrCreate("ignite2-back-cache-" + UUID.randomUUID().encodeBase62())
    }
}
