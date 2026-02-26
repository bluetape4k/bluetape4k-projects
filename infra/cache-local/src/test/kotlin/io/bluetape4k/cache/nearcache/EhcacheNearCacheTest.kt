package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.logging.KLogging
import javax.cache.expiry.EternalExpiryPolicy

class EhcacheNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> = JCaching.EhCache.getOrCreate(
        name = "back-cache-" + randomKey(),
        configuration = jcacheConfiguration {
            // setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration(MILLISECONDS, 50)))
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
        }
    )
}
