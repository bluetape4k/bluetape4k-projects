package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import org.junit.jupiter.api.Test
import javax.cache.expiry.EternalExpiryPolicy

class NearJCacheManagementMXBeanTest {

    companion object: KLogging()

    private fun newBackCache() = JCaching.Caffeine.getOrCreate<String, Any>(
        name = "back-cache-management-test-${System.nanoTime()}",
        configuration = jcacheConfiguration {
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
        }
    )

    private fun newNearCache(): NearJCache<String, Any> {
        val backCache = newBackCache()
        return NearJCache(NearJCacheConfig(), backCache)
    }

    @Test
    fun `getKeyType - Object 반환`() {
        val bean = NearJCacheManagementMXBean(newNearCache())
        bean.keyType shouldBeEqualTo "java.lang.Object"
    }

    @Test
    fun `getValueType - Object 반환`() {
        val bean = NearJCacheManagementMXBean(newNearCache())
        bean.valueType shouldBeEqualTo "java.lang.Object"
    }

    @Test
    fun `isReadThrough - false 반환`() {
        val bean = NearJCacheManagementMXBean(newNearCache())
        bean.isReadThrough.shouldBeFalse()
    }

    @Test
    fun `isWriteThrough - false 반환`() {
        val bean = NearJCacheManagementMXBean(newNearCache())
        bean.isWriteThrough.shouldBeFalse()
    }

    @Test
    fun `isStoreByValue - true 반환`() {
        val bean = NearJCacheManagementMXBean(newNearCache())
        bean.isStoreByValue shouldBeEqualTo true
    }

    @Test
    fun `isStatisticsEnabled - false 반환`() {
        val bean = NearJCacheManagementMXBean(newNearCache())
        bean.isStatisticsEnabled.shouldBeFalse()
    }

    @Test
    fun `isManagementEnabled - false 반환`() {
        val bean = NearJCacheManagementMXBean(newNearCache())
        bean.isManagementEnabled.shouldBeFalse()
    }
}
