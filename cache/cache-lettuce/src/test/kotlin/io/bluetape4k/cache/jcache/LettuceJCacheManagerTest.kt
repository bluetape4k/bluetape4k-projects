package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers.redisClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.cache.CacheException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceJCacheManagerTest {

    companion object: KLogging()

    private lateinit var manager: LettuceCacheManager
    private val registeredCache = mockk<LettuceJCache<Any, Any>>(relaxed = true)
    private val cacheMap = mockk<LettuceMap<ByteArray>>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(registeredCache, cacheMap)
        manager = LettuceCacheManager(
            redisClient = redisClient,
            classLoader = javaClass.classLoader,
            cacheProvider = LettuceCachingProvider(),
            properties = null,
            uri = null,
        )
    }

    @AfterEach
    fun teardown() {
        runCatching { manager.close() }
    }

    @Test
    fun `createCache and getCache`() {
        val config = lettuceCacheConfigOf<String, String>()
        val cache = manager.createCache("test-cache", config)
        cache.shouldNotBeNull()

        val retrieved = manager.getCache<String, String>("test-cache")
        retrieved.shouldNotBeNull()
    }

    @Test
    fun `typed getCache returns cache when key and value types match`() {
        val config = lettuceCacheConfigOf<String, String>()
        manager.createCache("typed-cache", config)

        val retrieved = manager.getCache("typed-cache", String::class.java, String::class.java)

        retrieved.shouldNotBeNull()
    }

    @Test
    fun `typed getCache throws when key type does not match`() {
        val config = lettuceCacheConfigOf<String, String>()
        manager.createCache("key-mismatch-cache", config)

        assertFailsWith<ClassCastException> {
            manager.getCache("key-mismatch-cache", Int::class.java, String::class.java)
        }
    }

    @Test
    fun `typed getCache throws when value type does not match`() {
        val config = lettuceCacheConfigOf<String, String>()
        manager.createCache("value-mismatch-cache", config)

        assertFailsWith<ClassCastException> {
            manager.getCache("value-mismatch-cache", String::class.java, Int::class.java)
        }
    }

    @Test
    fun `typed getCache validates type arguments`() {
        assertFailsWith<IllegalArgumentException> {
            manager.getCache<String, String>("typed-cache", null, String::class.java)
        }

        assertFailsWith<IllegalArgumentException> {
            manager.getCache<String, String>("typed-cache", String::class.java, null)
        }
    }

    @Test
    fun `typed getCache throws after close`() {
        manager.close()

        assertFailsWith<IllegalStateException> {
            manager.getCache("closed-cache", String::class.java, String::class.java)
        }
    }

    @Test
    fun `getCacheNames contains created cache`() {
        val config = lettuceCacheConfigOf<String, String>()
        manager.createCache("cache1", config)
        manager.createCache("cache2", lettuceCacheConfigOf<String, Int>())

        val names = manager.cacheNames
        names shouldContain "cache1"
        names shouldContain "cache2"
    }

    @Test
    fun `createCache throws when duplicate name`() {
        val config = lettuceCacheConfigOf<String, String>()
        manager.createCache("dup-cache", config)

        assertFailsWith<CacheException> {
            manager.createCache("dup-cache", config)
        }
    }

    @Test
    fun `destroyCache removes cache`() {
        val config = lettuceCacheConfigOf<String, String>()
        manager.createCache("to-destroy", config)
        manager.destroyCache("to-destroy")

        val retrieved = manager.getCache<String, String>("to-destroy")
        (retrieved == null || retrieved.isClosed).shouldBeTrue()
    }

    @Test
    fun `destroyCache propagates clear failure and keeps cache registered`() {
        val cacheName = "clear-failure-cache"
        val cause = IllegalStateException("redis clear failed")
        every { registeredCache.name } returns cacheName
        every { registeredCache.clear() } throws cause
        registerCache(cacheName, registeredCache)

        val thrown = assertFailsWith<CacheException> {
            manager.destroyCache(cacheName)
        }

        thrown.cause shouldBeEqualTo cause
        manager.getCache<Any, Any>(cacheName) shouldBeEqualTo registeredCache
        verify(exactly = 0) { registeredCache.close() }
    }

    @Test
    fun `destroyCache propagates close failure after clear and removes cache`() {
        val cacheName = "close-failure-cache"
        val cause = IllegalStateException("resource close failed")
        every { registeredCache.name } returns cacheName
        every { registeredCache.clear() } returns Unit
        every { registeredCache.close() } throws cause
        registerCache(cacheName, registeredCache)

        val thrown = assertFailsWith<CacheException> {
            manager.destroyCache(cacheName)
        }

        thrown.cause shouldBeEqualTo cause
        manager.getCache<Any, Any>(cacheName).shouldBeNull()
        verify { registeredCache.clear() }
        verify { registeredCache.close() }
    }

    @Test
    fun `destroyCache deletes redis data before same-name recreation`() {
        val cacheName = "recreate-after-destroy"
        val config = lettuceCacheConfigOf<String, String>()
        val cache = manager.createCache(cacheName, config)
        cache.put("key", "value")

        manager.destroyCache(cacheName)

        val recreated = manager.createCache(cacheName, config)
        recreated.get("key").shouldBeNull()
    }

    @Test
    fun `cache close exposes resource failure`() {
        val cause = IllegalStateException("connection close failed")
        every { cacheMap.mapKey } returns "close-resource-failure"
        val cache = LettuceJCache(
            map = cacheMap,
            cacheManager = manager,
            configuration = lettuceCacheConfigOf<String, String>(),
            closeResource = { throw cause },
        )

        val thrown = assertFailsWith<CacheException> {
            cache.close()
        }

        thrown.cause shouldBeEqualTo cause
        cache.isClosed.shouldBeTrue()
    }

    @Test
    fun `isClosed after close`() {
        manager.isClosed.shouldBeFalse()
        manager.close()
        manager.isClosed.shouldBeTrue()
    }

    @Test
    fun `operations throw after close`() {
        manager.close()
        assertFailsWith<IllegalStateException> {
            manager.createCache("after-close", lettuceCacheConfigOf<String, String>())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerCache(cacheName: String, cache: LettuceJCache<Any, Any>) {
        val field = LettuceCacheManager::class.java.getDeclaredField("caches").apply {
            isAccessible = true
        }
        val caches = field.get(manager) as MutableMap<String, LettuceJCache<*, *>>
        caches[cacheName] = cache
    }
}
