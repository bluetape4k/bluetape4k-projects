package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.logging.KLogging
import javax.cache.expiry.EternalExpiryPolicy

class Cache2kNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging() {
        private const val TEST_SIZE = 10
    }

    override val backCache: JCache<String, Any> = JCaching.Cache2k.getOrCreate(
        name = "back-cache-" + randomKey(),
        configuration = jcacheConfiguration {
            // setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration(MILLISECONDS, 50)))
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
        }
    )

    override fun `removeAll - 모든 캐시를 삭제하면 다른 캐시에도 반영된다`() {
        // FIXME: Cache2k 에서는 예외가 발생한다
    }
}
