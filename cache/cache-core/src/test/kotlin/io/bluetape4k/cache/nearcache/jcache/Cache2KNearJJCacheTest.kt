package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.assertFailsWith
import org.awaitility.core.ConditionTimeoutException
import org.junit.jupiter.api.Test
import javax.cache.expiry.EternalExpiryPolicy

class Cache2KNearJJCacheTest: AbstractNearJCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> = JCaching.Cache2k.getOrCreate(
        name = "back-cache-" + randomKey(),
        configuration = jcacheConfiguration {
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
        }
    )

    @Test
    override fun `removeAll - 모든 캐시를 삭제하면 다른 캐시에도 반영된다`() {
        assertFailsWith<ConditionTimeoutException> {
            super.`removeAll - 모든 캐시를 삭제하면 다른 캐시에도 반영된다`()
        }
    }
}
