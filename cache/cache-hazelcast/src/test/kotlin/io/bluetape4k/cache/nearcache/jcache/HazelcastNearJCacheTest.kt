package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.HazelcastCaches
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.HazelcastServers.hazelcastClient
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import com.hazelcast.nio.serialization.HazelcastSerializationException
import io.mockk.every
import io.mockk.mockk
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

        try {
            val failure = assertFailsWith<HazelcastSerializationException> {
                NearJCache(config, backCache)
            }
            failure.message shouldContain "MutableCacheEntryListenerConfiguration"
            failure.stackTraceToString() shouldContain "NotSerializableException"
            failure.stackTraceToString() shouldContain "JCacheEntryEventListener"
        } finally {
            backCache.close()
        }
    }

    @Test
    fun `public factory creates listener-free NearJCache on Hazelcast JCache`() {
        val cacheName = "hazelcast-public-near-jcache-" + Base58.randomString(6)
        val frontConfiguration = NearJCacheConfig.getDefaultFrontCacheConfiguration<String, String>()
        val frontCache = JCaching.Caffeine.getOrCreate<String, String>(
            "hazelcast-public-front-" + Base58.randomString(6),
            frontConfiguration,
        )
        val config = NearJCacheConfig<String, String>(cacheName = cacheName)
        val nearCache = HazelcastNearJCache(
            frontCache = frontCache,
            hazelcastInstance = hazelcastClient,
            nearCacheCfg = config,
        )

        try {
            nearCache.put("k", "v")
            nearCache.get("k") shouldBeEqualTo "v"
            nearCache.backCache.get("k") shouldBeEqualTo "v"

            nearCache.backCache.put("back-only", "from-back")
            nearCache.frontCache.clear()
            nearCache.get("back-only") shouldBeEqualTo "from-back"
            nearCache.frontCache.get("back-only") shouldBeEqualTo "from-back"
        } finally {
            nearCache.removeAll(setOf("k", "back-only"))
            nearCache.close()
        }

        frontCache.isClosed shouldBeEqualTo true
        nearCache.backCache.isClosed shouldBeEqualTo false
        hazelcastClient.lifecycleService.isRunning shouldBeEqualTo true
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
        val cleanupFailure = IllegalStateException("front close failed")
        val configurationClass = Configuration::class.java as Class<Configuration<String, String>>
        every {
            frontCache.getConfiguration(configurationClass)
        } returns storeByValueConfiguration
        every { frontCache.close() } throws cleanupFailure
        every {
            frontCacheManager.createCache<String, String, MutableConfiguration<String, String>>(cacheName, any())
        } returns frontCache

        val config = NearJCacheConfig<String, String>(
            cacheName = cacheName,
            cacheManagerFactory = Factory { frontCacheManager },
        )

        val error = assertFailsWith<IllegalArgumentException> {
            HazelcastCaches.nearJCache<String, String>(hazelcastClient, config)
        }

        error.suppressed.single() shouldBeEqualTo cleanupFailure
        verify(exactly = 1) { frontCache.close() }
    }

    @Test
    fun `factory creates listener-free NearJCache with read-through and write-through`() {
        val cacheName = "hazelcast-near-jcache-" + Base58.randomString(6)
        val cache =
            HazelcastCaches.nearJCache<String, String>(hazelcastClient, EXCLUSIVE_BACK_CACHE) {
                this.cacheName = cacheName
            }

        try {
            cache.put("k", "v")
            cache.get("k") shouldBeEqualTo "v"

            cache.getAndPut("compound", "v1").shouldBeNull()
            cache.getAndPut("compound", "v2") shouldBeEqualTo "v1"
            cache.getAndReplace("compound", "v3") shouldBeEqualTo "v2"
            cache.getAndRemove("compound") shouldBeEqualTo "v3"
            cache.get("compound").shouldBeNull()
            cache.backCache.get("compound").shouldBeNull()

            cache.clear()
            cache.get("k").shouldBeNull()
            cache.getDeeply("k").shouldBeNull()
        } finally {
            cache.clearAllCache()
            cache.close()
        }
    }

    @Test
    fun `기존 Hazelcast factory는 clear authority를 기본 거부한다`() {
        val cache = HazelcastCaches.nearJCache<String, String>(hazelcastClient) {
            cacheName = "hazelcast-near-jcache-default-authority-" + Base58.randomString(6)
        }

        try {
            assertFailsWith<SecurityException> { cache.clear() }
        } finally {
            cache.close()
        }
    }

    @Test
    fun `Hazelcast factory는 explicit exclusive authority overload를 전달한다`() {
        val cache = HazelcastCaches.nearJCache<String, String>(
            hazelcastClient,
            EXCLUSIVE_BACK_CACHE,
        ) {
            cacheName = "hazelcast-near-jcache-exclusive-authority-" + Base58.randomString(6)
        }

        try {
            cache.clearAllCache()
            cache.removeAll()
        } finally {
            cache.close()
        }
    }
}
