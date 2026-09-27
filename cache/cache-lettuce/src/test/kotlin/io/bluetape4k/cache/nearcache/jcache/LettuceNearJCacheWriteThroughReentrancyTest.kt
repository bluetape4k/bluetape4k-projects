package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.LettuceCaches
import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.TimeUnit

class LettuceNearJCacheWriteThroughReentrancyTest {

    companion object: KLogging()

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
            cache.putIfAbsent("absent-key", "absent-value").shouldBeTrue()

            cache.put("replace-key", "old-value")
            cache.replace("replace-key", "new-value").shouldBeTrue()

            cache.put("remove-key", "remove-value")
            cache.remove("remove-key").shouldBeTrue()

            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            elapsedMillis shouldBeLessThan 2_000L

            cache["key"] shouldBeEqualTo "value"
            cache["bulk-key"] shouldBeEqualTo "bulk-value"
            cache["absent-key"] shouldBeEqualTo "absent-value"
            cache["replace-key"] shouldBeEqualTo "new-value"
            cache["remove-key"].shouldBeNull()
        } finally {
            cache.close()
        }
    }
}
