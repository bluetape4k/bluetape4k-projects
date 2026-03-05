package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceCacheTest {

    companion object: KLogging()

    val provider = jcachingProvider<LettuceCachingProvider>()
    val manager = provider.getCacheManager(URI(RedisServers.redis.url), null)

    private lateinit var cache: LettuceCache<String, String>

    @BeforeEach
    fun beforeEach() {
        val cacheName = "test-cache-" + UUID.randomUUID().toString().take(8)
        @Suppress("UNCHECKED_CAST")
        cache = manager.createCache(
            cacheName,
            lettuceCacheConfigOf<String, String>()
        ) as LettuceCache<String, String>
    }

    @AfterEach
    fun afterEach() {
        runCatching { cache.close() }
    }

    @Test
    fun `put and get`() {
        cache.put("key1", "value1")
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `get returns null for missing key`() {
        cache.get("nonexistent").shouldBeNull()
    }

    @Test
    fun `containsKey`() {
        cache.containsKey("key1").shouldBeFalse()
        cache.put("key1", "value1")
        cache.containsKey("key1").shouldBeTrue()
    }

    @Test
    fun `remove key`() {
        cache.put("key1", "value1")
        cache.remove("key1").shouldBeTrue()
        cache.get("key1").shouldBeNull()
    }

    @Test
    fun `remove returns false when key not exists`() {
        cache.remove("nonexistent").shouldBeFalse()
    }

    @Test
    fun `remove with old value`() {
        cache.put("key1", "value1")
        cache.remove("key1", "wrongValue").shouldBeFalse()
        cache.remove("key1", "value1").shouldBeTrue()
        cache.get("key1").shouldBeNull()
    }

    @Test
    fun `putIfAbsent`() {
        cache.putIfAbsent("key1", "value1").shouldBeTrue()
        cache.putIfAbsent("key1", "value2").shouldBeFalse()
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `getAndPut`() {
        val old = cache.getAndPut("key1", "value1")
        old.shouldBeNull()
        val old2 = cache.getAndPut("key1", "value2")
        old2 shouldBeEqualTo "value1"
        cache.get("key1") shouldBeEqualTo "value2"
    }

    @Test
    fun `getAndRemove`() {
        cache.put("key1", "value1")
        val removed = cache.getAndRemove("key1")
        removed shouldBeEqualTo "value1"
        cache.get("key1").shouldBeNull()
    }

    @Test
    fun `replace with old and new value`() {
        cache.put("key1", "value1")
        cache.replace("key1", "wrongOld", "newValue").shouldBeFalse()
        cache.replace("key1", "value1", "newValue").shouldBeTrue()
        cache.get("key1") shouldBeEqualTo "newValue"
    }

    @Test
    fun `replace with new value only`() {
        cache.replace("key1", "value1").shouldBeFalse()
        cache.put("key1", "value1")
        cache.replace("key1", "newValue").shouldBeTrue()
        cache.get("key1") shouldBeEqualTo "newValue"
    }

    @Test
    fun `getAndReplace`() {
        cache.getAndReplace("key1", "value1").shouldBeNull()
        cache.put("key1", "value1")
        val old = cache.getAndReplace("key1", "value2")
        old shouldBeEqualTo "value1"
        cache.get("key1") shouldBeEqualTo "value2"
    }

    @Test
    fun `putAll and getAll`() {
        val map = mapOf("k1" to "v1", "k2" to "v2", "k3" to "v3")
        cache.putAll(map.toMutableMap())
        val result = cache.getAll(map.keys.toMutableSet())
        result shouldBeEqualTo map
    }

    @Test
    fun `removeAll with keys`() {
        val map = mapOf("k1" to "v1", "k2" to "v2", "k3" to "v3")
        cache.putAll(map.toMutableMap())
        cache.removeAll(setOf("k1", "k2").toMutableSet())
        cache.containsKey("k1").shouldBeFalse()
        cache.containsKey("k2").shouldBeFalse()
        cache.containsKey("k3").shouldBeTrue()
    }

    @Test
    fun `clear removes all entries`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clear()
        cache.containsKey("k1").shouldBeFalse()
        cache.containsKey("k2").shouldBeFalse()
    }

    @Test
    fun `cache name matches`() {
        cache.name.shouldNotBeNull()
    }

    @Test
    fun `isClosed after close`() {
        cache.isClosed.shouldBeFalse()
        cache.close()
        cache.isClosed.shouldBeTrue()
    }
}
