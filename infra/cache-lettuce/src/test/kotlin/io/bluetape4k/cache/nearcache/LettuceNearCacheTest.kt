package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.testcontainers.utility.Base58
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

class LettuceNearCacheTest: AbstractLettuceNearCacheTest() {
    companion object: KLogging()

    private lateinit var cache: LettuceNearCache<String>

    @BeforeAll
    fun createCache() {
        if (::cache.isInitialized) {
            cache.close()
        }
        cache = LettuceNearCache(
            redisClient = resp3Client,
            codec = StringCodec.UTF8,
            config = LettuceNearCacheConfig(cacheName = "test-near-cache-" + Base58.randomString(6))
        )
        // BeforeEach의 flushdb 이후 생성
    }

    @AfterAll
    fun tearDown() {
        if (::cache.isInitialized) cache.close()
    }

    // ---- 기본 CRUD ----

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() {
        verifyGetMiss { cache.get(it) }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - read-through`() {
        verifyPutAndGet(
            put = { k, v -> cache.put(k, v) },
            get = { cache.get(it) }
        )
    }

    @Test
    fun `put - Redis에도 반영됨`() {
        cache.put("k", "v")
        // prefix key로 확인
        directCommands.get("${cache.cacheName}:k") shouldBeEqualTo "v"
    }

    @Test
    fun `get - front miss 시 Redis에서 읽어 front populate`() {
        // prefix key로 직접 설정해야 cache.get()이 찾을 수 있음
        directCommands.set("${cache.cacheName}:remote-key", "remote-val")
        cache.localCacheSize() shouldBeEqualTo 0L

        cache.get("remote-key") shouldBeEqualTo "remote-val"
        cache.localCacheSize() shouldBeEqualTo 1L // front populated
    }

    @Test
    fun `putAll and getAll`() {
        verifyGetAll(
            putAll = { cache.putAll(it) },
            getAll = { cache.getAll(it) }
        )
    }

    @Test
    fun `remove - front + Redis 삭제`() {
        verifyRemove(
            put = { k, v -> cache.put(k, v) },
            get = { cache.get(it) },
            remove = { cache.remove(it) }
        )
    }

    @Test
    fun `removeAll - 여러 키 삭제`() {
        verifyRemoveAll(
            putAll = { cache.putAll(it) },
            removeAll = { cache.removeAll(it) },
            get = { cache.get(it) }
        )
    }

    @Test
    fun `containsKey - 키 존재 여부 확인`() {
        verifyContainsKey(
            put = { k, v -> cache.put(k, v) },
            containsKey = { cache.containsKey(it) },
            remove = { cache.remove(it) }
        )
    }

    @Test
    fun `putIfAbsent - 캐시 값 없으면 추가, 있으면 기존 값 반환`() {
        verifyPutIfAbsent(
            putIfAbsent = { k, v -> cache.putIfAbsent(k, v) },
            get = { cache.get(it) }
        )
    }

    @Test
    fun `replace - 캐시 값 교체`() {
        verifyReplace(
            put = { k, v -> cache.put(k, v) },
            replace = { k, v -> cache.replace(k, v) },
            get = { cache.get(it) }
        )
    }

    @Test
    fun `replace(key, oldValue, newValue) - 값이 일치할 때만 교체`() {
        cache.put("k", "old")
        cache.replace("k", "wrong", "new").shouldBeFalse()
        cache.replace("k", "old", "new").shouldBeTrue()
        cache.get("k") shouldBeEqualTo "new"
    }

    @Test
    fun `getAndRemove - 캐시 값 조회 및 삭제`() {
        verifyGetAndRemove(
            put = { k, v -> cache.put(k, v) },
            getAndRemove = { cache.getAndRemove(it) },
            get = { cache.get(it) }
        )
    }

    @Test
    fun `getAndReplace - 캐시 값 조회 및 교체`() {
        verifyGetAndReplace(
            put = { k, v -> cache.put(k, v) },
            getAndReplace = { k, v -> cache.getAndReplace(k, v) },
            get = { cache.get(it) }
        )
    }

    // ---- clearLocal / clearAll ----

    @Test
    fun `clearLocal - 로컬만 초기화, Redis 유지`() {
        verifyClearLocal(
            put = { k, v -> cache.put(k, v) },
            clearLocal = { cache.clearLocal() },
            localSize = { cache.localCacheSize() },
            // prefix key로 Redis 직접 확인
            getFromRedis = { directCommands.get("${cache.cacheName}:$it") }
        )
    }

    @Test
    fun `clearAll - 로컬 + Redis 초기화`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L
        // prefix key로 삭제 확인
        directCommands.get("${cache.cacheName}:k1").shouldBeNull()
        directCommands.get("${cache.cacheName}:k2").shouldBeNull()
    }

    @Test
    fun `clearAll - 다른 cacheName의 데이터는 유지됨`() {
        val otherCache =
            LettuceNearCache(
                redisClient = resp3Client,
                codec = StringCodec.UTF8,
                config = LettuceNearCacheConfig(cacheName = "other-cache-" + Base58.randomString(6))
            )
        otherCache.use { other ->
            cache.put("shared-key", "from-main-cache")
            other.put("shared-key", "from-other-cache")

            cache.clearAll()

            // main cache 데이터 삭제됨
            cache.get("shared-key").shouldBeNull()
            directCommands.get("${cache.cacheName}:shared-key").shouldBeNull()

            // other cache 데이터 유지됨
            other.get("shared-key") shouldBeEqualTo "from-other-cache"
            directCommands.get("${other.cacheName}:shared-key") shouldBeEqualTo "from-other-cache"
        }
    }

    // ---- TTL ----

    @Test
    fun `Redis TTL - TTL이 있는 캐시 설정`() {
        val ttlCacheName = "ttl-test-" + Base58.randomString(6)
        val ttlCache =
            LettuceNearCache(
                redisClient = resp3Client,
                codec = StringCodec.UTF8,
                config =
                    LettuceNearCacheConfig(
                        cacheName = ttlCacheName,
                        redisTtl = Duration.ofSeconds(2)
                    )
            )
        ttlCache.use { c ->
            c.put("ttl-key", "ttl-val")
            c.get("ttl-key") shouldBeEqualTo "ttl-val"
            // prefix key로 TTL 확인
            val ttl = directCommands.ttl("$ttlCacheName:ttl-key")
            (ttl > 0L).shouldBeTrue()
        }
    }

    // ---- redisSize ----

    @Test
    fun `redisSize - cacheName에 속한 Redis key 개수`() {
        cache.put("s1", "v1")
        cache.put("s2", "v2")
        cache.put("s3", "v3")
        cache.backCacheSize() shouldBeEqualTo 3L
        cache.remove("s2")
        cache.backCacheSize() shouldBeEqualTo 2L
    }

    // ---- lifecycle ----

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = LettuceNearCache<String>(resp3Client)
        c.close()
        c.close() // 두 번 호출해도 예외 없어야 함
    }

    // ---- 오류 복원력 ----

    @Test
    fun `put - Redis 쓰기 실패 시 local cache를 오염시키지 않는다`() {
        val failingCache =
            LettuceNearCache(
                redisClient = resp3Client,
                codec = failingValueCodec(),
                config = LettuceNearCacheConfig(cacheName = "fail-put-" + Base58.randomString(6))
            )

        failingCache.use { broken ->
            assertWriteFailure {
                broken.put("k", "boom")
            }

            broken.localCacheSize() shouldBeEqualTo 0L
            broken.containsKey("k").shouldBeFalse()
        }
    }

    @Test
    fun `putAll - Redis 쓰기 실패 시 local cache를 오염시키지 않는다`() {
        val failingCache =
            LettuceNearCache(
                redisClient = resp3Client,
                codec = failingValueCodec(),
                config = LettuceNearCacheConfig(cacheName = "fail-putall-" + Base58.randomString(6))
            )
        failingCache.use { c ->
            assertFailsWith<Exception> { c.putAll(mapOf("k1" to "v1", "k2" to "v2")) }
            c.localCacheSize() shouldBeEqualTo 0L
        }
    }

    // ---- 동시성 ----

    @Test
    fun `get - MultithreadingTester 동일 key 첫 조회 경쟁에서도 read-through 결과가 유지된다`() {
        directCommands.set("${cache.cacheName}:mt-key", "mt-val")

        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add {
                cache.get("mt-key") shouldBeEqualTo "mt-val"
            }
            .run()

        cache.localCacheSize() shouldBeEqualTo 1L
        cache.get("mt-key") shouldBeEqualTo "mt-val"
    }


    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `get - StructuredTaskScopeTester 병렬 조회에서도 값 일관성을 유지한다`() {
        val key = "structured-read-through"
        directCommands.set("${cache.cacheName}:$key", "structured-value")

        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                cache.get(key) shouldBeEqualTo "structured-value"
            }
            .run()

        cache.localCacheSize() shouldBeEqualTo 1L
        cache.get(key) shouldBeEqualTo "structured-value"
    }

    @Test
    fun `putIfAbsent - MultithreadingTester 경쟁 상황에서도 단 한 번만 저장된다`() {
        val storeCount = AtomicInteger(0)
        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add {
                val existing = cache.putIfAbsent("race-key", "v-${Thread.currentThread().threadId()}")
                if (existing == null) storeCount.incrementAndGet()
            }.run()
        storeCount.get() shouldBeEqualTo 1
        cache.get("race-key").shouldNotBeNull()
    }

    // ---- putIfAbsent TTL 원자성 ----

    @Test
    fun `putIfAbsent - TTL이 있는 경우 단일 명령으로 TTL까지 함께 저장한다`() {
        val ttlMs = 5_000L
        val ttlCacheName = "pia-ttl-" + Base58.randomString(6)
        val ttlCache =
            LettuceNearCache(
                redisClient = resp3Client,
                codec = StringCodec.UTF8,
                config = LettuceNearCacheConfig(cacheName = ttlCacheName, redisTtl = Duration.ofMillis(ttlMs))
            )
        ttlCache.use { c ->
            c.putIfAbsent("pia-key", "pia-val").shouldBeNull()
            val pttl = directCommands.pttl("$ttlCacheName:pia-key")
            (pttl > 0L && pttl <= ttlMs).shouldBeTrue()
        }
    }

    // ---- Millisecond TTL ----

    @Test
    fun `Redis TTL - millisecond 단위도 보존된다`() {
        val ttlMs = 5_000L
        val ttlCacheName = "ms-ttl-" + Base58.randomString(6)
        val ttlCache =
            LettuceNearCache(
                redisClient = resp3Client,
                codec = StringCodec.UTF8,
                config = LettuceNearCacheConfig(cacheName = ttlCacheName, redisTtl = Duration.ofMillis(ttlMs))
            )
        ttlCache.use { c ->
            c.put("ms-key", "ms-val")
            val pttl = directCommands.pttl("$ttlCacheName:ms-key")
            (pttl > 0L && pttl <= ttlMs).shouldBeTrue()
        }
    }

    // ---- Config 검증 ----

    @Test
    fun `NearCacheConfig는 잘못된 설정값을 즉시 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfig<String, String>(cacheName = "")
        }
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheConfig<String, String>(cacheName = "bad:name")
        }
        assertFailsWith<IllegalArgumentException> {
            lettuceNearCacheConfig<String, String> {
                cacheName = "valid"
                maxLocalSize = 0L
            }
        }
    }

    // ---- helpers ----

    private fun failingValueCodec(): RedisCodec<String, String> =
        object: RedisCodec<String, String> {
            private val delegate = StringCodec.UTF8

            override fun decodeKey(bytes: ByteBuffer): String = delegate.decodeKey(bytes)

            override fun decodeValue(bytes: ByteBuffer): String = delegate.decodeValue(bytes)

            override fun encodeKey(key: String): ByteBuffer = delegate.encodeKey(key)

            override fun encodeValue(value: String): ByteBuffer =
                throw IllegalStateException("encode failure for test")
        }

    private fun assertWriteFailure(block: () -> Unit) {
        val thrown = runCatching(block).exceptionOrNull()
        thrown.shouldNotBeNull()
        generateSequence(thrown) { it.cause }
            .any { it is IllegalStateException && it.message == "encode failure for test" }
            .shouldBeTrue()
    }
}
