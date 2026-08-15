package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.lettuce.core.codec.StringCodec
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import io.bluetape4k.assertions.assertFailsWith
import java.net.URI
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import javax.cache.CacheException
import javax.cache.configuration.MutableCacheEntryListenerConfiguration
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryUpdatedListener
import javax.cache.processor.EntryProcessor
import javax.cache.processor.EntryProcessorException
import javax.cache.processor.MutableEntry

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceJCacheTest {

    companion object: KLogging()

    val provider = jcachingProvider<LettuceCachingProvider>()
    val manager = provider.getCacheManager(URI(RedisServers.redis.url), null)

    private lateinit var cache: LettuceJCache<String, String>

    @BeforeEach
    fun beforeEach() {
        val cacheName = "test-cache-" + UUID.randomUUID().toString().take(8)
        @Suppress("UNCHECKED_CAST")
        cache = manager.createCache(
            cacheName,
            lettuceCacheConfigOf<String, String>()
        ) as LettuceJCache<String, String>
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
    fun `putIfAbsent applies ttl when configured`() {
        val ttlCache = manager.createCache(
            "ttl-cache-" + UUID.randomUUID().toString().take(8),
            lettuceCacheConfigOf<String, String>(ttlSeconds = 1)
        ) as LettuceJCache<String, String>

        try {
            ttlCache.putIfAbsent("key1", "value1").shouldBeTrue()
            ttlCache.get("key1") shouldBeEqualTo "value1"

            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                RedisServers.redisClient.connect(StringCodec.UTF8).use { connection ->
                    (connection.sync().ttl(ttlCache.name) > 0L).shouldBeTrue()
                }
            }
        } finally {
            runCatching { ttlCache.close() }
        }
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

    @Test
    fun `invoke updates entry through EntryProcessor`() {
        cache.put("key1", "value1")

        val result = cache.invoke(
            "key1",
            EntryProcessor<String, String, String> { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                val next = entry.value + "-updated"
                entry.setValue(next)
                next
            })

        result shouldBeEqualTo "value1-updated"
        cache.get("key1") shouldBeEqualTo "value1-updated"
    }

    @Test
    fun `invokeAll returns result per key`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")

        val results = cache.invokeAll(
            mutableSetOf("k1", "k2"),
            EntryProcessor<String, String, String> { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                val next = entry.value + "-x"
                entry.setValue(next)
                next
            }
        )

        results["k1"]?.get() shouldBeEqualTo "v1-x"
        results["k2"]?.get() shouldBeEqualTo "v2-x"
        cache.get("k1") shouldBeEqualTo "v1-x"
        cache.get("k2") shouldBeEqualTo "v2-x"
    }

    @Test
    fun `invoke serializes read modify write across cache instances`() {
        val mapName = "invoke-atomic-" + UUID.randomUUID().toString().take(8)
        val first = standaloneCache(mapName)
        val second = standaloneCache(mapName)
        val nextCache = AtomicInteger()
        val totalInvocations = 8 * 10

        try {
            first.put("counter", 0)

            MultithreadingTester()
                .workers(8)
                .rounds(10)
                .add {
                    val target = if (nextCache.getAndIncrement() % 2 == 0) first else second
                    target.invoke(
                        "counter",
                        EntryProcessor<String, Int, Int> { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                            val current = entry.value ?: 0
                            Thread.sleep(2)
                            val updated = current + 1
                            entry.setValue(updated)
                            updated
                        }
                    )
                }
                .run()

            first.get("counter") shouldBeEqualTo totalInvocations
        } finally {
            runCatching { first.clear() }
            runCatching { first.close() }
            runCatching { second.close() }
        }
    }

    @Test
    fun `invoke exception does not commit a partial entry update`() {
        cache.put("key1", "original")

        assertFailsWith<EntryProcessorException> {
            cache.invoke(
                "key1",
                EntryProcessor<String, String, String> { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                    entry.setValue("transient")
                    error("processor failed")
                }
            )
        }

        cache.get("key1") shouldBeEqualTo "original"
    }

    @Test
    fun `invokeAll keeps per key result and exception contracts`() {
        cache.putAll(mapOf("ok" to "value", "bad" to "value").toMutableMap())

        val results = cache.invokeAll(
            setOf("ok", "bad"),
            EntryProcessor<String, String, String> { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                if (entry.key == "bad") error("bad entry")
                val updated = entry.value + "-updated"
                entry.setValue(updated)
                updated
            }
        )

        results["ok"]?.get() shouldBeEqualTo "value-updated"
        assertFailsWith<EntryProcessorException> { results["bad"]?.get() }
        cache.get("ok") shouldBeEqualTo "value-updated"
        cache.get("bad") shouldBeEqualTo "value"
    }

    @Test
    fun `invoke refreshes ttl and dispatches updated listener event`() {
        val ttlCache = manager.createCache(
            "invoke-ttl-" + UUID.randomUUID().toString().take(8),
            lettuceCacheConfigOf<String, String>(ttlSeconds = 60)
        ) as LettuceJCache<String, String>
        val updatedEvents = CopyOnWriteArrayList<String>()
        val listener = object : CacheEntryUpdatedListener<String, String> {
            override fun onUpdated(events: Iterable<CacheEntryEvent<out String, out String>>) {
                events.forEach { event -> updatedEvents += "${event.key}:${event.value}" }
            }
        }

        try {
            ttlCache.registerCacheEntryListener(
                MutableCacheEntryListenerConfiguration({ listener }, null, false, true)
            )
            ttlCache.put("key1", "value1")
            updatedEvents.clear()

            ttlCache.invoke(
                "key1",
                EntryProcessor<String, String, String> { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                    val updated = entry.value + "-updated"
                    entry.setValue(updated)
                    updated
                }
            )

            ttlCache.get("key1") shouldBeEqualTo "value1-updated"
            updatedEvents shouldBeEqualTo listOf("key1:value1-updated")
            RedisServers.redisClient.connect(StringCodec.UTF8).use { connection ->
                (connection.sync().ttl(ttlCache.name) > 0L).shouldBeTrue()
            }
        } finally {
            runCatching { ttlCache.close() }
        }
    }

    private fun standaloneCache(mapName: String): LettuceJCache<String, Int> {
        val connection = RedisServers.redisClient.connect(LettuceCacheManager.STRING_BYTES_CODEC)
        val map = LettuceMap<ByteArray>(connection, mapName)
        val configuration = lettuceCacheConfigOf<String, Int>(codec = LettuceBinaryCodecs.lz4Fory<Int>())
        return LettuceJCache(
            map = map,
            codec = LettuceBinaryCodecs.lz4Fory<Int>(),
            cacheManager = manager as LettuceCacheManager,
            configuration = configuration,
            closeResource = { connection.close() },
        )
    }

    @Test
    fun `iterator and entry traversal support non String key cache with keyDecoder`() {
        val intKeyCache = manager.createCache(
            "int-key-cache-" + UUID.randomUUID().toString().take(8),
            lettuceCacheConfigOf<Int, String>(
                keyDecoder = String::toInt
            )
        ) as LettuceJCache<Int, String>

        try {
            intKeyCache.put(1, "one")
            val entry = intKeyCache.iterator().next()
            entry.key shouldBeEqualTo 1
            entry.value shouldBeEqualTo "one"
        } finally {
            runCatching { intKeyCache.close() }
        }
    }

    @Test
    fun `iterator throws CacheException for non String key cache without keyDecoder`() {
        val intKeyCache = manager.createCache(
            "int-key-cache-" + UUID.randomUUID().toString().take(8),
            lettuceCacheConfigOf<Int, String>()
        ) as LettuceJCache<Int, String>

        try {
            intKeyCache.put(1, "one")
            assertFailsWith<CacheException> {
                intKeyCache.iterator()
            }
        } finally {
            runCatching { intKeyCache.close() }
        }
    }
}
