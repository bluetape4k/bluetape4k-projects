package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.IgniteJCaching
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import java.util.*

class IgniteNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging()

    override val nearCacheCfg1 = NearCacheConfig<String, Any>(checkExpiryPeriod = 1_000)
    override val nearCacheCfg2 = NearCacheConfig<String, Any>(checkExpiryPeriod = 1_000)

    override val backCache: JCache<String, Any> by lazy {
        IgniteJCaching.getOrCreate("ignite2-back-cache-" + UUID.randomUUID().encodeBase62())
    }
}
