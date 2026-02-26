package io.bluetape4k.cache.jcache

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import javax.cache.Cache
import javax.cache.integration.CacheLoader
import javax.cache.integration.CacheWriter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class JCacheReadWriteThroughExample {

    companion object: KLogging() {
        @JvmStatic
        private fun randomKey(): String = Fakers.randomUuid().encodeBase62()

        @JvmStatic
        private fun randomString() =
            Fakers.randomString(512, 1024)
    }

    private val backCache = JCaching.EhCache.getOrCreate<String, Any>("back-cache")

    private fun <K, V> cacheLoader(source: Cache<K, V>): CacheLoader<K, V> =
        object: CacheLoader<K, V> {
            override fun load(key: K): V = source.get(key)
            override fun loadAll(keys: Iterable<K>): MutableMap<K, V> = source.getAll(keys.toSet())
        }

    private fun <K, V> cacheWriter(dest: Cache<K, V>): CacheWriter<K, V> =
        object: CacheWriter<K, V> {
            override fun write(entry: Cache.Entry<out K, out V>) {
                dest.put(entry.key, entry.value)
            }

            override fun writeAll(entries: MutableCollection<Cache.Entry<out K, out V>>) {
                dest.putAll(entries.associate { it.key to it.value })
            }

            @Suppress("UNCHECKED_CAST")
            override fun delete(key: Any) {
                val k = key as? K
                k?.let { dest.remove(it) }
            }

            @Suppress("UNCHECKED_CAST")
            override fun deleteAll(keys: MutableCollection<*>) {
                val ks = keys.mapNotNull { it as? K }
                dest.removeAll(ks.toSet())
            }
        }

    private val configuration = jcacheConfiguration<String, Any> {
        isReadThrough = true
        setCacheLoaderFactory { cacheLoader(backCache) }

        isWriteThrough = true
        setCacheWriterFactory { cacheWriter(backCache) }
    }

    private val frontCache = JCaching.Caffeine.getOrCreate("front-cache", configuration)

    @BeforeEach
    fun setup() {
        frontCache.clear()
        backCache.clear()
    }

    @AfterAll
    fun afterAll() {
        frontCache.close()
    }

    @Test
    fun `get cache entry with read through write through`() {
        val key = randomKey()
        val value = randomString()

        frontCache.put(key, value)
        backCache.get(key) shouldBeEqualTo value

        frontCache.remove(key)
        backCache.containsKey(key).shouldBeFalse()
    }

    @Test
    fun `read write through with bulk cache entries`() {
        val entries = List(100) {
            val key = randomKey()
            val value = randomString()
            key to value
        }.toMap()

        frontCache.putAll(entries)

        val remoteEntries = backCache.getAll(entries.keys)
        remoteEntries.toSortedMap() shouldBeEqualTo entries.toSortedMap()
    }

    @Test
    fun `clear not applied write through`() {
        val key = randomKey()
        val value = randomString()

        frontCache.put(key, value)
        backCache.containsKey(key).shouldBeTrue()

        // clear 는 write through를 하지 않는다
        frontCache.clear()
        backCache.containsKey(key).shouldBeTrue()
    }

    @Test
    fun `removeAll with write through`() {
        val key = randomKey()
        val value = randomString()

        frontCache.put(key, value)
        backCache.containsKey(key).shouldBeTrue()

        frontCache.removeAll()
        backCache.containsKey(key).shouldBeFalse()
    }

    @Test
    fun `remote 에만 존재하는 key를 near 에서 containsKey는 false`() {
        val key = randomKey()
        val value = randomString()

        frontCache.containsKey(key).shouldBeFalse()
        backCache.put(key, value)

        // NOTE: remote 에 key를 추가하면 near에서도 read 는 가능해도 containsKey 는 load 하지 않는다
        // Layered 가 되었다면 순차적으로 caches 에게 containsKey를 수행해야 한다
        frontCache.containsKey(key).shouldBeFalse()
    }
}
