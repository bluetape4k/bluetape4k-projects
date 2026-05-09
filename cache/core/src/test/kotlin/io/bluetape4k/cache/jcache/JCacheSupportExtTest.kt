package io.bluetape4k.cache.jcache

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import javax.cache.Cache

class JCacheSupportExtTest {

    companion object: KLogging()

    @Test
    fun `jcacheManagerOf - qualified name 으로 CacheManager 반환`() {
        val manager = jcacheManagerOf(CaffeineCachingProvider::class.qualifiedName!!)
        manager.shouldNotBeNull()
    }

    @Test
    fun `jcacheConfigurationOf - cacheLoaderFactory 설정 시 람다 실행`() {
        val backCache = JCaching.Caffeine.getOrCreate<String, Any>("jcache-loader-source-${System.nanoTime()}")
        backCache.put("loader-key", "loader-value")

        val config = jcacheConfigurationOf<String, Any>(
            cacheLoaderFactory = { backCache.cacheLoader() },
            isReadThrough = true,
        )

        config.shouldNotBeNull()
        config.isReadThrough shouldBeEqualTo true

        backCache.close()
    }

    @Test
    fun `jcacheConfigurationOf - cacheWriterFactory 설정 시 람다 실행`() {
        val backCache = JCaching.Caffeine.getOrCreate<String, Any>("jcache-writer-dest-${System.nanoTime()}")

        val config = jcacheConfigurationOf<String, Any>(
            cacheWriterFactory = { backCache.cacheWriter() },
            isWriteThrough = true,
        )

        config.shouldNotBeNull()
        config.isWriteThrough shouldBeEqualTo true

        backCache.close()
    }

    @Test
    fun `cacheLoader - load 와 loadAll 이 동작한다`() {
        val cache = JCaching.Caffeine.getOrCreate<String, Any>("jcache-loader-test-${System.nanoTime()}")
        cache.put("key1", "value1")
        cache.put("key2", "value2")

        val loader = cache.cacheLoader()

        loader.load("key1") shouldBeEqualTo "value1"
        loader.load("key2") shouldBeEqualTo "value2"

        val all = loader.loadAll(mutableListOf("key1", "key2"))
        all["key1"] shouldBeEqualTo "value1"
        all["key2"] shouldBeEqualTo "value2"

        cache.close()
    }

    @Test
    fun `cacheWriter - write 와 delete 가 동작한다`() {
        val cache = JCaching.Caffeine.getOrCreate<String, Any>("jcache-writer-test-${System.nanoTime()}")

        val writer = cache.cacheWriter()

        val entry = object: Cache.Entry<String, Any> {
            override fun getKey(): String = "write-key"
            override fun getValue(): Any = "write-value"
            override fun <T: Any?> unwrap(clazz: Class<T>?): T = clazz!!.cast(this)
        }

        writer.write(entry)
        cache.get("write-key") shouldBeEqualTo "write-value"

        writer.delete("write-key")
        val result = cache.get("write-key")
        // delete 후 null 이어야 한다
        result shouldBeEqualTo null

        cache.close()
    }

    @Test
    fun `cacheWriter - writeAll 과 deleteAll 이 동작한다`() {
        val cache = JCaching.Caffeine.getOrCreate<String, Any>("jcache-writer-all-test-${System.nanoTime()}")

        val writer = cache.cacheWriter()

        val entries = mutableListOf<Cache.Entry<String, Any>>(
            object: Cache.Entry<String, Any> {
                override fun getKey(): String = "wk1"
                override fun getValue(): Any = "wv1"
                override fun <T: Any?> unwrap(clazz: Class<T>?): T = clazz!!.cast(this)
            },
            object: Cache.Entry<String, Any> {
                override fun getKey(): String = "wk2"
                override fun getValue(): Any = "wv2"
                override fun <T: Any?> unwrap(clazz: Class<T>?): T = clazz!!.cast(this)
            }
        )

        @Suppress("UNCHECKED_CAST")
        writer.writeAll(entries as MutableCollection<javax.cache.Cache.Entry<out String, out Any>>)
        cache.get("wk1") shouldBeEqualTo "wv1"
        cache.get("wk2") shouldBeEqualTo "wv2"

        writer.deleteAll(mutableListOf("wk1", "wk2"))
        cache.get("wk1") shouldBeEqualTo null
        cache.get("wk2") shouldBeEqualTo null

        cache.close()
    }
}
