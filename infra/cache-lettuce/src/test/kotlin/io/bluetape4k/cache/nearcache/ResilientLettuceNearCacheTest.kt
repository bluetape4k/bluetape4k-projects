package io.bluetape4k.cache.nearcache

import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.awaitility.kotlin.untilNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class ResilientLettuceNearCacheTest: AbstractLettuceNearCacheTest() {

    private lateinit var cache: ResilientLettuceNearCache<String>

    @BeforeEach
    fun createCache() {
        if (::cache.isInitialized) cache.close()
        cache = ResilientLettuceNearCache(
            redisClient = resp3Client,
            codec = StringCodec.UTF8,
            config = resilientLettuceNearCacheConfig {
                cacheName = "resilient-near-cache"
                retryMaxAttempts = 2
                retryWaitDuration = Duration.ofMillis(100)
            },
        )
    }

    @AfterAll
    fun tearDown() {
        if (::cache.isInitialized) cache.close()
    }

    // ---- 기본 CRUD ----

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() {
        cache.get("missing-key").shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - front cache 즉시 반영`() {
        cache.put("key1", "value1")
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - write-behind - 잠시 후 Redis에도 반영됨`() {
        cache.put("wb-key", "wb-val")
        // front cache 즉시 확인
        cache.get("wb-key") shouldBeEqualTo "wb-val"
        // Redis는 write-behind로 비동기 반영 → awaitility로 폴링
        await.atMost(3, TimeUnit.SECONDS).untilNotNull {
            directCommands.get("${cache.cacheName}:wb-key")
        }
        directCommands.get("${cache.cacheName}:wb-key") shouldBeEqualTo "wb-val"
    }

    @Test
    fun `get - front miss 시 Redis에서 읽어 front populate`() {
        directCommands.set("${cache.cacheName}:remote-key", "remote-val")
        cache.localCacheSize() shouldBeEqualTo 0L

        cache.get("remote-key") shouldBeEqualTo "remote-val"
        cache.localCacheSize() shouldBeEqualTo 1L
    }

    @Test
    fun `putAll and getAll`() {
        val data = mapOf("a" to "1", "b" to "2", "c" to "3")
        cache.putAll(data)
        val result = cache.getAll(setOf("a", "b", "c", "x"))
        result["a"] shouldBeEqualTo "1"
        result["b"] shouldBeEqualTo "2"
        result["c"] shouldBeEqualTo "3"
        result["x"].shouldBeNull()
    }

    @Test
    fun `remove - front 즉시 삭제, Redis write-behind`() {
        cache.put("key1", "value1")
        cache.get("key1").shouldNotBeNull()

        cache.remove("key1")
        // front 즉시 반영
        cache.get("key1").shouldBeNull()
        // Redis도 write-behind로 삭제됨 확인
        await.atMost(3, TimeUnit.SECONDS).untilNull {
            directCommands.get("${cache.cacheName}:key1")
        }
    }

    @Test
    fun `removeAll - 여러 키 삭제`() {
        cache.putAll(mapOf("a" to "1", "b" to "2", "c" to "3"))
        cache.removeAll(setOf("a", "b"))
        cache.get("a").shouldBeNull()
        cache.get("b").shouldBeNull()
        cache.get("c") shouldBeEqualTo "3"
    }

    @Test
    fun `containsKey`() {
        cache.put("keyX", "valX")
        cache.containsKey("keyX").shouldBeTrue()
        cache.containsKey("nonexistent").shouldBeFalse()
        cache.remove("keyX")
        cache.containsKey("keyX").shouldBeFalse()
    }

    @Test
    fun `putIfAbsent`() {
        cache.putIfAbsent("key", "first").shouldBeNull()
        cache.get("key") shouldBeEqualTo "first"
        cache.putIfAbsent("key", "second") shouldBeEqualTo "first"
        cache.get("key") shouldBeEqualTo "first"
    }

    @Test
    fun `getAndRemove`() {
        cache.put("key", "value")
        cache.getAndRemove("key") shouldBeEqualTo "value"
        cache.get("key").shouldBeNull()
        cache.getAndRemove("key").shouldBeNull()
    }

    @Test
    fun `getAndReplace`() {
        cache.getAndReplace("missing", "val").shouldBeNull()
        cache.put("key", "old")
        cache.getAndReplace("key", "new") shouldBeEqualTo "old"
        cache.get("key") shouldBeEqualTo "new"
    }

    // ---- clearLocal / clearAll ----

    @Test
    fun `clearLocal - 로컬만 초기화, Redis 유지`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        // Redis write-behind 완료 대기
        await.atMost(3, TimeUnit.SECONDS).untilNotNull {
            directCommands.get("${cache.cacheName}:k1")
        }

        cache.localCacheSize() shouldBeEqualTo 2L
        cache.clearLocal()
        cache.localCacheSize() shouldBeEqualTo 0L
        directCommands.get("${cache.cacheName}:k1").shouldNotBeNull()
    }

    @Test
    fun `clearAll - front + Redis 모두 초기화 (write-behind)`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        // write-behind 완료 대기
        await.atMost(3, TimeUnit.SECONDS).untilNotNull {
            directCommands.get("${cache.cacheName}:k1")
        }

        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L
        // ClearBack도 write-behind → 잠시 후 Redis 삭제
        await.atMost(3, TimeUnit.SECONDS).untilNull {
            directCommands.get("${cache.cacheName}:k1")
        }
        directCommands.get("${cache.cacheName}:k2").shouldBeNull()
    }

    @Test
    fun `clearAll - 다른 cacheName의 데이터는 유지됨`() {
        val otherCache = ResilientLettuceNearCache(
            redisClient = resp3Client,
            codec = StringCodec.UTF8,
            config = resilientLettuceNearCacheConfig {
                cacheName = "other-resilient-cache-" + Base58.randomString(6)
            },
        )
        otherCache.use { other ->
            cache.put("shared-key", "from-main")
            other.put("shared-key", "from-other")
            // write-behind 완료 대기
            await.atMost(3, TimeUnit.SECONDS).untilNotNull {
                directCommands.get("${other.cacheName}:shared-key")
            }

            cache.clearAll()
            cache.get("shared-key").shouldBeNull()
            other.get("shared-key") shouldBeEqualTo "from-other"
        }
    }

    // ---- TTL ----

    @Test
    fun `Redis TTL - write-behind 완료 후 TTL 확인`() {
        val ttlCacheName = "ttl-resilient-sync-test-" + Base58.randomString(6)
        val ttlCache = ResilientLettuceNearCache(
            redisClient = resp3Client,
            codec = StringCodec.UTF8,
            config = resilientLettuceNearCacheConfig {
                cacheName = ttlCacheName
                redisTtl = Duration.ofSeconds(10)
            },
        )
        ttlCache.use { c ->
            c.put("ttl-key", "ttl-val")
            c.get("ttl-key") shouldBeEqualTo "ttl-val"
            // write-behind 완료 후 TTL 확인
            await.atMost(3, TimeUnit.SECONDS).until {
                (directCommands.ttl("${ttlCacheName}:ttl-key") ?: -1L) > 0L
            }
            (directCommands.ttl("${ttlCacheName}:ttl-key") > 0L).shouldBeTrue()
        }
    }

    // ---- backCacheSize ----

    @Test
    fun `backCacheSize - write-behind 완료 후 key 개수 확인`() {
        cache.put("s1", "v1")
        cache.put("s2", "v2")
        cache.put("s3", "v3")
        await.atMost(3, TimeUnit.SECONDS).until { cache.backCacheSize() == 3L }
        cache.backCacheSize() shouldBeEqualTo 3L

        cache.remove("s2")
        await.atMost(3, TimeUnit.SECONDS).until { cache.backCacheSize() == 2L }
        cache.backCacheSize() shouldBeEqualTo 2L
    }

    // ---- write-behind ordering ----

    @Test
    fun `write-behind queue ordering - 순서대로 Redis에 반영됨`() {
        repeat(5) { i ->
            cache.put("order-key-$i", "val-$i")
        }
        // 모든 write-behind 완료 대기
        await.atMost(5, TimeUnit.SECONDS).until {
            (0 until 5).all {
                directCommands.get("${cache.cacheName}:order-key-$it") != null
            }
        }
        repeat(5) { i ->
            directCommands.get("${cache.cacheName}:order-key-$i") shouldBeEqualTo "val-$i"
        }
    }

    // ---- lifecycle ----

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = ResilientLettuceNearCache<String>(resp3Client)
        c.close()
        c.close()
        c.isClosed.shouldBeTrue()
    }

    @Test
    fun `queue full rollback keeps remove and clearAll readable`() {
        val limited = ResilientLettuceNearCache(
            redisClient = resp3Client,
            codec = StringCodec.UTF8,
            config = resilientLettuceNearCacheConfig {
                cacheName = "resilient-queue-full-" + Base58.randomString(6)
                writeQueueCapacity = 1
            },
        )

        try {
            val c = limited
            val consumerThreadField = c.findField("consumerThread")
            val queueField = c.findField("queue")

            @Suppress("UNCHECKED_CAST")
            val queue = queueField.get(c) as LinkedBlockingQueue<BackCacheCommand<String, String>>
            val consumerThread = consumerThreadField.get(c) as Thread

            consumerThread.interrupt()
            consumerThread.join(500)
            queue.clear()
            queue.offer(BackCacheCommand.Put("occupied", "value")).shouldBeTrue()
            (queue.remainingCapacity() == 0).shouldBeTrue()

            directCommands.set("${c.cacheName}:blocked", "value")
            c.remove("blocked")
            c.containsKey("blocked").shouldBeTrue()
            c.get("blocked") shouldBeEqualTo "value"

            directCommands.set("${c.cacheName}:clear-key", "clear-value")
            c.clearAll()
            c.get("clear-key") shouldBeEqualTo "clear-value"
        } finally {
            limited.close()
        }
    }

    private fun Any.findField(name: String) =
        javaClass.declaredFields.firstOrNull { it.name == name || it.name.startsWith(name) }
            ?.apply { isAccessible = true }
            ?: error("Field not found: $name in ${javaClass.name}")
}
