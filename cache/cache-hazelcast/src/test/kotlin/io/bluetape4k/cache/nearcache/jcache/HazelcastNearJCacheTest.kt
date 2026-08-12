package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.HazelcastCaches
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.HazelcastServers.hazelcastClient
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import com.hazelcast.nio.serialization.HazelcastSerializationException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration

class HazelcastNearJCacheTest {

    companion object: KLogging()

    private fun backCache(cacheName: String): JCache<String, Any> {
        val config = MutableConfiguration<String, Any>().apply {
            setTypes(String::class.java, Any::class.java)
        }
        return HazelcastCaches.jcache(HazelcastServers.hazelcastClient, cacheName, config)
    }

    @Test
    fun `direct listener-backed NearJCache is unsupported for Hazelcast JCache`() {
        val cacheName = "hazelcast-backcache-" + Base58.randomString(6)
        val config = NearJCacheConfig<String, Any>(cacheName = "hazelcast-front-" + Base58.randomString(6))
        val backCache = backCache(cacheName)

        val failure = assertFailsWith<HazelcastSerializationException> {
            NearJCache(config, backCache)
        }
        failure.message shouldContain "MutableCacheEntryListenerConfiguration"
        failure.stackTraceToString() shouldContain "NotSerializableException"
        failure.stackTraceToString() shouldContain "JCacheEntryEventListener"
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `factory closes front cache when NearJCache construction fails`() {
        val cacheName = "hazelcast-near-jcache-failure-" + Base58.randomString(6)
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val frontCacheManager = mockk<CacheManager>()
        val storeByValueConfiguration = MutableConfiguration<String, String>().apply {
            setStoreByValue(true)
        }
        val configurationClass = Configuration::class.java as Class<Configuration<String, String>>
        every {
            frontCache.getConfiguration(configurationClass)
        } returns storeByValueConfiguration
        every { frontCache.close() } just runs
        every {
            frontCacheManager.createCache<String, String, MutableConfiguration<String, String>>(cacheName, any())
        } returns frontCache

        val config = NearJCacheConfig<String, String>(
            cacheName = cacheName,
            cacheManagerFactory = Factory { frontCacheManager },
        )

        assertFailsWith<IllegalArgumentException> {
            HazelcastCaches.nearJCache<String, String>(hazelcastClient, config)
        }

        verify(exactly = 1) { frontCache.close() }
    }

    @Test
    fun `factory creates listener-free NearJCache with read-through and write-through`() {
        val cacheName = "hazelcast-near-jcache-" + Base58.randomString(6)
        val cache =
            HazelcastCaches.nearJCache<String, String>(hazelcastClient) {
                this.cacheName = cacheName
            }

        try {
            cache.put("k", "v")
            cache.get("k") shouldBeEqualTo "v"

            cache.clear()
            cache.get("k").shouldBeNull()
            cache.getDeeply("k").shouldBeNull()
        } finally {
            cache.clearAllCache()
            cache.close()
        }
    }
}
