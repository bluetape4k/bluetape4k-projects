package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cache.LettuceCaches
import io.bluetape4k.cache.RedisServers
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

class LettuceNearJCacheWriteThroughReentrancyTest {

    @Test
    fun `동기 Lettuce write-through은 inline listener 재진입으로 timeout되지 않는다`() {
        val cache = LettuceCaches.nearJCache<String, String>(RedisServers.redisClient) {
            cacheName = "near-jcache-reentrancy-${UUID.randomUUID()}"
            isSynchronous = true
            syncRemoteTimeout = 100L
        }

        try {
            val startedAt = System.nanoTime()
            cache.put("key", "value")
            cache.putAll(mapOf("bulk-key" to "bulk-value"))
            check(cache.putIfAbsent("absent-key", "absent-value"))
            cache.put("replace-key", "old-value")
            check(cache.replace("replace-key", "new-value"))
            cache.put("remove-key", "remove-value")
            check(cache.remove("remove-key"))
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

            check(elapsedMillis < 2_000L) {
                "synchronous Lettuce CRUD write-through exceeded bounded completion: ${elapsedMillis}ms"
            }
            cache.get("key") shouldBeEqualTo "value"
            cache.get("bulk-key") shouldBeEqualTo "bulk-value"
            cache.get("absent-key") shouldBeEqualTo "absent-value"
            cache.get("replace-key") shouldBeEqualTo "new-value"
            cache.get("remove-key") shouldBeEqualTo null
        } finally {
            cache.close()
        }
    }
}
