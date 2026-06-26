package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers.redisClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import io.bluetape4k.assertions.assertFailsWith
import javax.cache.CacheException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceJCacheManagerTest {

    companion object: KLogging()

    private lateinit var manager: LettuceCacheManager

    @BeforeEach
    fun setup() {
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
}
