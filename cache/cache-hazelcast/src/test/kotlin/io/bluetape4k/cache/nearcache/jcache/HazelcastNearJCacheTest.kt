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
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
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
        failure.stackTraceToString() shouldContain "com.github.benmanes.caffeine.jcache.CacheProxy"
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
