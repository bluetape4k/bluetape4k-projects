package io.bluetape4k.cache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cache.RedisServers
import io.bluetape4k.codec.Base58
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.lettuce.core.codec.StringCodec
import io.mockk.every
import io.mockk.mockk
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.cache.CacheException
import javax.cache.configuration.MutableCacheEntryListenerConfiguration
import javax.cache.configuration.MutableConfiguration
import javax.cache.event.CacheEntryUpdatedListener
import javax.cache.processor.EntryProcessorException
import javax.cache.processor.MutableEntry
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceJCacheTest {

    companion object: KLogging()

    private val provider = jcachingProvider<LettuceCachingProvider>()
    private val manager = provider.getCacheManager(URI(RedisServers.redis.url), null).shouldNotBeNull()

    private lateinit var cache: LettuceJCache<String, String>

    @BeforeEach
    fun beforeEach() {
        val cacheName = "test-cache-" + Base58.randomString(8)
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
    fun `null serializer result is reported as CacheException without payload`() {
        val map = mockk<LettuceMap<ByteArray>>()
        every { map.mapKey } returns "null-deserialize-cache"
        every { map.get("key") } returns byteArrayOf(0x5a)

        val serializer = object: BinarySerializer {
            override fun serialize(graph: Any?): ByteArray = byteArrayOf(0x01)
            override fun <T: Any> deserialize(bytes: ByteArray?): T? = null
        }

        val brokenCache = LettuceJCache<String, String>(
            map = map,
            codec = LettuceBinaryCodec<Any>(serializer),
            cacheManager = manager as LettuceCacheManager,
            configuration = lettuceCacheConfigOf(),
        )

        val exception = assertFailsWith<CacheException> {
            brokenCache.get("key")
        }
        exception.message shouldBeEqualTo "LettuceCache[null-deserialize-cache] 값 역직렬화 결과가 null입니다."
    }

    @Test
    fun `containsKey - contains cache item`() {
        cache.containsKey("key1").shouldBeFalse()
        cache.put("key1", "value1")
        cache.containsKey("key1").shouldBeTrue()
    }

    @Test
    fun `remove - remove cache item`() {
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
    fun `putIfAbsent - put if absent cache item`() {
        cache.putIfAbsent("key1", "value1").shouldBeTrue()
        cache.putIfAbsent("key1", "value2").shouldBeFalse()
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `lock lease seconds must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            lettuceCacheConfigOf<String, String>(lockLeaseSeconds = 0)
        }
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
    fun `getAndPut - get and put cache item`() {
        val old = cache.getAndPut("key1", "value1")
        old.shouldBeNull()
        val old2 = cache.getAndPut("key1", "value2")
        old2 shouldBeEqualTo "value1"
        cache.get("key1") shouldBeEqualTo "value2"
    }

    @Test
    fun `getAndRemove - get and remove cache item`() {
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
    fun `getAndReplace - get and replace cache item`() {
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
            key = "key1",
            entryProcessor = { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                val next = entry.value + "-updated"
                entry.value = next
                next
            }
        )

        result shouldBeEqualTo "value1-updated"
        cache.get("key1") shouldBeEqualTo "value1-updated"
    }

    @Test
    fun `invoke removes an existing entry through the lock-owned delete`() {
        cache.put("key1", "value1")

        cache.invoke(
            key = "key1",
            entryProcessor = { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                entry.remove()
                "removed"
            }
        ) shouldBeEqualTo "removed"

        cache.containsKey("key1").shouldBeFalse()

        cache.invoke(
            key = "missing",
            entryProcessor = { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                entry.remove()
                "missing"
            }
        ) shouldBeEqualTo "missing"
        cache.containsKey("missing").shouldBeFalse()
    }

    @Test
    fun `entry processor uses the default lease for a generic JCache configuration`() {
        val mapName = "invoke-default-lease-" + UUID.randomUUID().toString().take(8)
        val connection = RedisServers.redisClient.connect(LettuceCacheManager.STRING_BYTES_CODEC)

        val fallbackCache = LettuceJCache(
            map = LettuceMap(connection, mapName),
            codec = LettuceBinaryCodecs.default<Int>(),
            cacheManager = manager as LettuceCacheManager,
            configuration = MutableConfiguration<String, Int>().setTypes(String::class.java, Int::class.java),
            closeResource = { connection.close() },
        )

        try {
            fallbackCache.invoke(
                key = "key",
                entryProcessor = { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                    entry.value = 1
                    1
                }
            ) shouldBeEqualTo 1
            fallbackCache.get("key") shouldBeEqualTo 1
        } finally {
            runCatching { fallbackCache.close() }
        }
    }

    @Test
    fun `invokeAll returns result per key`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")

        val results = cache.invokeAll(
            keys = mutableSetOf("k1", "k2"),
            entryProcessor = { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                val next = entry.value + "-x"
                entry.value = next
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
                        key = "counter",
                        entryProcessor = { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                            val current = entry.value ?: 0
                            Thread.sleep(2)
                            val updated = current + 1
                            entry.value = updated
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
    fun `invoke serializes repeated read modify write on one cache instance`() {
        val mapName = "invoke-shared-connection-" + UUID.randomUUID().toString().take(8)
        val target = standaloneCache(mapName)
        val totalInvocations = 12 * 20

        try {
            target.put("counter", 0)

            MultithreadingTester()
                .workers(12)
                .rounds(20)
                .add {
                    target.invoke(
                        key = "counter",
                        entryProcessor = { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                            val updated = (entry.value ?: 0) + 1
                            entry.value = updated
                        },
                    )
                }
                .run()

            target.get("counter") shouldBeEqualTo totalInvocations
        } finally {
            runCatching { target.clear() }
            runCatching { target.close() }
        }
    }

    @Test
    fun `invoke rejects stale commit after lease expiry and lock handoff`() {
        val mapName = "invoke-lease-expiry-" + UUID.randomUUID().toString().take(8)
        val first = standaloneCache(mapName, lockLeaseSeconds = 1)
        val second = standaloneCache(mapName, lockLeaseSeconds = 1)
        val processorEntered = CountDownLatch(1)
        val releaseProcessor = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val lockKey = "$mapName:__bluetape4k:lock"

        try {
            first.put("counter", 0)
            val firstInvocation = executor.submit<Throwable?> {
                try {
                    first.invoke(
                        key = "counter",
                        entryProcessor = { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                            processorEntered.countDown()
                            check(releaseProcessor.await(5, TimeUnit.SECONDS))
                            entry.value = 1
                        }
                    )
                    null
                } catch (error: Throwable) {
                    error
                }
            }

            check(processorEntered.await(5, TimeUnit.SECONDS))
            await atMost 5.seconds until {
                RedisServers.redisClient.connect(StringCodec.UTF8).use { connection ->
                    connection.sync().exists(lockKey) == 0L
                }
            }

            second.invoke(
                key = "counter",
                entryProcessor = { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                    entry.value = 2
                    2
                }
            ) shouldBeEqualTo 2

            releaseProcessor.countDown()
            firstInvocation.get(5, TimeUnit.SECONDS).shouldBeInstanceOf<IllegalStateException>()
            first.get("counter") shouldBeEqualTo 2
        } finally {
            releaseProcessor.countDown()
            executor.shutdownNow()
            runCatching { first.clear() }
            runCatching { first.close() }
            runCatching { second.close() }
        }
    }

    @Test
    fun `invoke rejects stale removal after lease expiry and lock handoff`() {
        val mapName = "invoke-remove-lease-expiry-" + UUID.randomUUID().toString().take(8)
        val first = standaloneCache(mapName, lockLeaseSeconds = 1)
        val second = standaloneCache(mapName, lockLeaseSeconds = 1)
        val processorEntered = CountDownLatch(1)
        val releaseProcessor = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val lockKey = "$mapName:__bluetape4k:lock"

        try {
            first.put("counter", 0)
            val firstInvocation = executor.submit<Throwable?> {
                try {
                    first.invoke(
                        key = "counter",
                        entryProcessor = { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                            processorEntered.countDown()
                            check(releaseProcessor.await(5, TimeUnit.SECONDS))
                            entry.remove()
                        }
                    )
                    null
                } catch (error: Throwable) {
                    error
                }
            }

            check(processorEntered.await(5, TimeUnit.SECONDS))
            await atMost 5.seconds until {
                RedisServers.redisClient.connect(StringCodec.UTF8).use { connection ->
                    connection.sync().exists(lockKey) == 0L
                }
            }

            second.invoke(
                key = "counter",
                entryProcessor = { entry: MutableEntry<String, Int>, _: Array<out Any?> ->
                    entry.value = 2
                    2
                }
            ) shouldBeEqualTo 2

            releaseProcessor.countDown()
            firstInvocation.get(5, TimeUnit.SECONDS).shouldBeInstanceOf<IllegalStateException>()
            first.get("counter") shouldBeEqualTo 2
        } finally {
            releaseProcessor.countDown()
            executor.shutdownNow()
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
                key = "key1",
                entryProcessor = { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                    entry.value = "transient"
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
            keys = setOf("ok", "bad"),
            entryProcessor = { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                if (entry.key == "bad") error("bad entry")
                val updated = entry.value + "-updated"
                entry.value = updated
                updated
            }
        )

        results["ok"]?.get() shouldBeEqualTo "value-updated"

        assertFailsWith<EntryProcessorException> {
            results["bad"]?.get()
        }
        cache.get("ok") shouldBeEqualTo "value-updated"
        cache.get("bad") shouldBeEqualTo "value"
    }

    @Test
    fun `invoke refreshes ttl and dispatches updated listener event`() {
        val ttlCache = manager.createCache(
            "invoke-ttl-" + Base58.randomString(8),
            lettuceCacheConfigOf<String, String>(ttlSeconds = 60)
        ) as LettuceJCache<String, String>
        val updatedEvents = CopyOnWriteArrayList<String>()
        val listener = CacheEntryUpdatedListener<String, String> { events ->
            events.forEach { event -> updatedEvents += "${event.key}:${event.value}" }
        }

        try {
            ttlCache.registerCacheEntryListener(
                MutableCacheEntryListenerConfiguration({ listener }, null, false, true)
            )
            ttlCache.put("key1", "value1")
            updatedEvents.clear()

            ttlCache.invoke(
                key = "key1",
                entryProcessor = { entry: MutableEntry<String, String>, _: Array<out Any?> ->
                    val updated = entry.value + "-updated"
                    entry.value = updated
                }
            )

            ttlCache.get("key1") shouldBeEqualTo "value1-updated"
            updatedEvents shouldBeEqualTo listOf("key1:value1-updated")

            RedisServers.redisClient.connect(StringCodec.UTF8).use { connection ->
                connection.sync().ttl(ttlCache.name) shouldBeGreaterThan 0L
            }
        } finally {
            runCatching { ttlCache.close() }
        }
    }

    private fun standaloneCache(
        mapName: String,
        lockLeaseSeconds: Long = LettuceCacheConfig.DEFAULT_LOCK_LEASE_SECONDS,
    ): LettuceJCache<String, Int> {
        val connection = RedisServers.redisClient.connect(LettuceCacheManager.STRING_BYTES_CODEC)
        val map = LettuceMap<ByteArray>(connection, mapName)
        val configuration = lettuceCacheConfigOf<String, Int>(
            codec = LettuceBinaryCodecs.default<Int>(),
            lockLeaseSeconds = lockLeaseSeconds,
        )
        return LettuceJCache(
            map = map,
            codec = LettuceBinaryCodecs.default<Int>(),
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
