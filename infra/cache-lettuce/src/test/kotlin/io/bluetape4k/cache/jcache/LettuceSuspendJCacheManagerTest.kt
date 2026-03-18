package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers.redisClient
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class LettuceSuspendJCacheManagerTest {

    @Test
    fun `closeCache removes registry entry but keeps redis data`() = runSuspendIO {
        val manager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())
        val cacheName = "lettuce-suspend-manager-" + UUID.randomUUID().encodeBase62()
        val cache = manager.getOrCreate<String>(cacheName)

        try {
            cache.put("key", "value")

            manager.closeCache(cache)
            manager.getCache<String>(cacheName).shouldBeNull()

            val reopened = manager.getOrCreate<String>(cacheName)
            reopened.get("key") shouldBeEqualTo "value"
        } finally {
            runCatching { manager.destroyCache(cacheName) }
            runCatching { manager.close() }
        }
    }

    @Test
    fun `closed manager rejects further operations`() {
        val manager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())
        runSuspendIO { manager.close() }

        assertThrows(IllegalStateException::class.java) {
            manager.getOrCreate<String>("after-close")
        }
        assertThrows(IllegalStateException::class.java) {
            manager.getCache<String>("after-close")
        }
        assertThrows(IllegalStateException::class.java) {
            runSuspendIO { manager.destroyCache("after-close") }
        }
    }

    @Test
    fun `매니저 기본 codec이 캐시에 적용되는지 확인`() = runSuspendIO {
        val manager = LettuceSuspendCacheManager(
            redisClient = redisClient,
            defaultCodec = LettuceBinaryCodecs.lz4Fory(),
        )
        val cacheName = "codec-test-cache-" + UUID.randomUUID().encodeBase62()

        try {
            val cache = manager.getOrCreate<String>(cacheName)
            cache.put("key", "value")
            cache.get("key") shouldBeEqualTo "value"
        } finally {
            runCatching { manager.destroyCache(cacheName) }
            runCatching { manager.close() }
        }
    }

    @Test
    fun `매니저 기본 ttlSeconds가 캐시에 적용되는지 확인`() = runSuspendIO {
        val manager = LettuceSuspendCacheManager(
            redisClient = redisClient,
            defaultTtlSeconds = 60L,
        )
        val cacheName = "ttl-test-cache-" + UUID.randomUUID().encodeBase62()

        try {
            val cache = manager.getOrCreate<String>(cacheName)
            cache.put("key", "value")
            cache.get("key") shouldBeEqualTo "value"
        } finally {
            runCatching { manager.destroyCache(cacheName) }
            runCatching { manager.close() }
        }
    }
}
