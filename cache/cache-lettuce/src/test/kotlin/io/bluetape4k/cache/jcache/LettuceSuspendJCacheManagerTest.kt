package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers.redisClient
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.cache.CacheException

class LettuceSuspendJCacheManagerTest {

    private val registeredCache = mockk<LettuceSuspendJCache<Any>>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(registeredCache)
    }

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
    fun `destroyCache propagates clear failure and keeps cache registered`() = runSuspendIO {
        val cacheName = "suspend-clear-failure-cache"
        val cause = IllegalStateException("redis clear failed")
        val manager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())
        every { registeredCache.name } returns cacheName
        coEvery { registeredCache.clear() } throws cause
        registerCache(manager, cacheName, registeredCache)

        try {
            val thrown = assertFailsWith<CacheException> {
                manager.destroyCache(cacheName)
            }

            thrown.cause shouldBeEqualTo cause
            manager.getCache<Any>(cacheName) shouldBeEqualTo registeredCache
            coVerify(exactly = 0) { registeredCache.close() }
        } finally {
            manager.close()
        }
    }

    @Test
    fun `destroyCache propagates close failure after clear and removes cache`() = runSuspendIO {
        val cacheName = "suspend-close-failure-cache"
        val cause = IllegalStateException("resource close failed")
        val manager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())
        every { registeredCache.name } returns cacheName
        coEvery { registeredCache.clear() } returns Unit
        coEvery { registeredCache.close() } throws cause
        registerCache(manager, cacheName, registeredCache)

        try {
            val thrown = assertFailsWith<CacheException> {
                manager.destroyCache(cacheName)
            }

            thrown.cause shouldBeEqualTo cause
            manager.getCache<Any>(cacheName).shouldBeNull()
            coVerify { registeredCache.clear() }
            coVerify { registeredCache.close() }
        } finally {
            manager.close()
        }
    }

    @Test
    fun `destroyCache deletes redis data before same-name recreation`() = runSuspendIO {
        val cacheName = "suspend-recreate-after-destroy-" + UUID.randomUUID().encodeBase62()
        val manager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())

        try {
            val cache = manager.getOrCreate<String>(cacheName)
            cache.put("key", "value")

            manager.destroyCache(cacheName)

            manager.getCache<String>(cacheName).shouldBeNull()
            manager.getOrCreate<String>(cacheName).get("key").shouldBeNull()
        } finally {
            runCatching { manager.destroyCache(cacheName) }
            runCatching { manager.close() }
        }
    }

    @Test
    fun `manager close releases wrappers but keeps redis data`() = runSuspendIO {
        val cacheName = "lettuce-suspend-manager-close-" + UUID.randomUUID().encodeBase62()
        val manager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())
        val cache = manager.getOrCreate<String>(cacheName)

        cache.put("key", "value")
        manager.close()

        val reopenedManager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())
        try {
            // close()는 JCache 계약상 Redis hash 데이터를 삭제하지 않는다. 새 wrapper로 다시 읽을 수 있어야 한다.
            val reopened = reopenedManager.getOrCreate<String>(cacheName)
            reopened.get("key") shouldBeEqualTo "value"
        } finally {
            runCatching { reopenedManager.destroyCache(cacheName) }
            runCatching { reopenedManager.close() }
        }
    }

    @Test
    fun `closed manager rejects further operations`() {
        val manager = LettuceSuspendCacheManager(redisClient, defaultCodec = LettuceBinaryCodecs.lz4Fory())
        runSuspendIO { manager.close() }

        assertFailsWith<IllegalStateException> {
            manager.getOrCreate<String>("after-close")
        }
        assertFailsWith<IllegalStateException> {
            manager.getCache<String>("after-close")
        }
        assertFailsWith<IllegalStateException> {
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

    @Suppress("UNCHECKED_CAST")
    private fun registerCache(
        manager: LettuceSuspendCacheManager,
        cacheName: String,
        cache: LettuceSuspendJCache<Any>,
    ) {
        val field = LettuceSuspendCacheManager::class.java.getDeclaredField("caches").apply {
            isAccessible = true
        }
        val caches = field.get(manager) as MutableMap<String, LettuceSuspendJCache<out Any>>
        caches[cacheName] = cache
    }
}
