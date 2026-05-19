package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.HazelcastCaches
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.HazelcastServers.hazelcastClient
import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldBeEqualTo
import com.hazelcast.nio.serialization.HazelcastSerializationException
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.time.Duration

class HazelcastSuspendNearJCacheTest {

    companion object: KLoggingChannel()

    private fun frontCache(): SuspendJCache<String, Any> =
        CaffeineSuspendJCache {
            expireAfterAccess(Duration.ofMinutes(5))
            maximumSize(100_000)
        }

    @Test
    fun `direct listener-backed SuspendNearJCache is unsupported for Hazelcast JCache`() = runSuspendIO {
        val backCache =
            HazelcastCaches.suspendJCache<String, Any>(
                HazelcastServers.hazelcastClient,
                "hazelcast-suspend-back-" + Base58.randomString(6),
            )
        val frontCache = frontCache()

        val failure = assertFailsWith<HazelcastSerializationException> {
            SuspendNearJCache.invoke(frontCache, backCache)
        }
        failure.message shouldContain "MutableCacheEntryListenerConfiguration"
        failure.stackTraceToString() shouldContain "NotSerializableException"
        failure.stackTraceToString() shouldContain "io.bluetape4k.cache.jcache.CaffeineSuspendJCache"
    }

    @Test
    fun `factory creates listener-free SuspendNearJCache with read-through and write-through`() = runSuspendIO {
        val cacheName = "hazelcast-suspend-near-jcache-" + Base58.randomString(6)
        val cache =
            HazelcastCaches.suspendNearJCache<String, String>(hazelcastClient) {
                this.cacheName = cacheName
            }

        try {
            cache.put("k", "v")
            cache.get("k") shouldBeEqualTo "v"

            cache.clear()
            cache.get("k") shouldBeEqualTo "v"
        } finally {
            cache.clearAll()
            cache.close()
        }
    }
}
