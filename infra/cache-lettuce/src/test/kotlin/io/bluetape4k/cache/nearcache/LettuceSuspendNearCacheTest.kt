package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.nio.ByteBuffer
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

class LettuceSuspendNearCacheTest: AbstractLettuceNearCacheTest() {

    companion object: KLoggingChannel()

    private lateinit var cache: LettuceSuspendNearCache<String>

    @BeforeEach
    fun createCache() {
        if (::cache.isInitialized) cache.close()

        cache = LettuceSuspendNearCache(
            redisClient = resp3Client,
            codec = StringCodec.UTF8,
            config = LettuceNearCacheConfig(cacheName = "test-near-suspend-cache")
        )
    }

    @AfterAll
    fun tearDown() {
        if (::cache.isInitialized) cache.close()
    }

    // ---- 기본 CRUD ----

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() = runTest {
        cache.get("missing-key").shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - read-through`() = runTest {
        cache.put("key1", "value1")
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - Redis에도 반영됨`() = runTest {
        cache.put("k", "v")
        // prefix key로 확인
        directCommands.get("${cache.cacheName}:k") shouldBeEqualTo "v"
    }

    @Test
    fun `get - front miss 시 Redis에서 읽어 front populate`() = runTest {
        // prefix key로 직접 설정
        directCommands.set("${cache.cacheName}:remote-key", "remote-val")
        cache.localCacheSize() shouldBeEqualTo 0L

        cache.get("remote-key") shouldBeEqualTo "remote-val"
        cache.localCacheSize() shouldBeEqualTo 1L // front populated
    }

    @Test
    fun `putAll and getAll`() = runTest {
        val data = mapOf("a" to "1", "b" to "2", "c" to "3")
        cache.putAll(data)
        val result = cache.getAll(setOf("a", "b", "c", "x"))
        result["a"] shouldBeEqualTo "1"
        result["b"] shouldBeEqualTo "2"
        result["c"] shouldBeEqualTo "3"
        result["x"].shouldBeNull()
    }

    @Test
    fun `remove - front + Redis 삭제`() = runTest {
        cache.put("key1", "value1")
        cache.get("key1").shouldNotBeNull()
        cache.remove("key1")
        cache.get("key1").shouldBeNull()
    }

    @Test
    fun `removeAll - 여러 키 삭제`() = runTest {
        cache.putAll(mapOf("a" to "1", "b" to "2", "c" to "3"))
        cache.removeAll(setOf("a", "b"))
        cache.get("a").shouldBeNull()
        cache.get("b").shouldBeNull()
        cache.get("c") shouldBeEqualTo "3"
    }

    @Test
    fun `containsKey`() = runTest {
        cache.put("keyX", "valX")
        cache.containsKey("keyX").shouldBeTrue()
        cache.containsKey("nonexistent").shouldBeFalse()
        cache.remove("keyX")
        cache.containsKey("keyX").shouldBeFalse()
    }

    @Test
    fun `putIfAbsent`() = runTest {
        cache.putIfAbsent("key", "first").shouldBeNull() // 새로 저장 → null 반환
        cache.get("key") shouldBeEqualTo "first"
        cache.putIfAbsent("key", "second") shouldBeEqualTo "first" // 기존 존재 → 기존 값 반환
        cache.get("key") shouldBeEqualTo "first"
    }

    @Test
    fun `replace`() = runTest {
        cache.replace("noKey", "val").shouldBeFalse()
        cache.put("key", "old")
        cache.replace("key", "new").shouldBeTrue()
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `replace(key, oldValue, newValue) - 값이 일치할 때만 교체`() = runTest {
        cache.put("k", "old")
        cache.replace("k", "wrong", "new").shouldBeFalse()
        cache.replace("k", "old", "new").shouldBeTrue()
        cache.get("k") shouldBeEqualTo "new"
    }

    @Test
    fun `getAndRemove`() = runTest {
        cache.put("key", "value")
        cache.getAndRemove("key") shouldBeEqualTo "value"
        cache.get("key").shouldBeNull()
        cache.getAndRemove("key").shouldBeNull()
    }

    @Test
    fun `getAndReplace`() = runTest {
        cache.getAndReplace("missing", "val").shouldBeNull()
        cache.put("key", "old")
        cache.getAndReplace("key", "new") shouldBeEqualTo "old"
        cache.get("key") shouldBeEqualTo "new"
    }

    // ---- clearLocal / clearAll ----

    @Test
    fun `clearLocal - 로컬만 초기화, Redis 유지`() = runTest {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.localCacheSize() shouldBeEqualTo 2L
        cache.clearFrontCache()
        cache.localCacheSize() shouldBeEqualTo 0L
        // prefix key로 Redis 데이터 유지 확인
        directCommands.get("${cache.cacheName}:k1").shouldNotBeNull()
    }

    @Test
    fun `clearAll - 프론트 캐시 + 백엔드 캐시 초기화`() = runTest {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L
        // prefix key로 삭제 확인
        directCommands.get("${cache.cacheName}:k1").shouldBeNull()
        directCommands.get("${cache.cacheName}:k2").shouldBeNull()
    }

    @Test
    fun `clearAll - 다른 cacheName의 데이터는 유지됨`() = runTest {
        val otherCache = LettuceSuspendNearCache(
            redisClient = resp3Client,
            codec = StringCodec.UTF8,
            config = LettuceNearCacheConfig(cacheName = "other-suspend-cache-" + Base58.randomString(6))
        )
        otherCache.use { other ->
            cache.put("shared-key", "from-main")
            other.put("shared-key", "from-other")

            cache.clearAll()

            cache.get("shared-key").shouldBeNull()
            other.get("shared-key") shouldBeEqualTo "from-other"
        }
    }

    // ---- TTL ----

    @Test
    fun `Redis TTL - TTL이 있는 캐시 설정`() = runTest {
        val ttlCacheName = "ttl-suspend-test-" + Base58.randomString(6)
        val ttlCache = LettuceSuspendNearCache(
            redisClient = resp3Client, codec = StringCodec.UTF8, config = LettuceNearCacheConfig(
                cacheName = ttlCacheName, redisTtl = Duration.ofSeconds(2)
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
    fun `backCacheSize - cacheName에 속한 Redis key 개수`() = runTest {
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
        val c = LettuceSuspendNearCache<String>(resp3Client)
        c.close()
        c.close()
    }

    // ---- 오류 복원력 ----

    @Test
    fun `put - Redis 쓰기 실패 시 local cache를 오염시키지 않는다`() = runTest {
        val failingCache = LettuceSuspendNearCache(
            redisClient = resp3Client,
            codec = failingValueCodec(),
            config = LettuceNearCacheConfig(cacheName = "fail-put-susp-" + Base58.randomString(6))
        )

        failingCache.use { c ->
            assertWriteFailure {
                c.put("k", "v")
            }
            c.localCacheSize() shouldBeEqualTo 0L
        }
    }

    @Test
    fun `putAll - Redis 쓰기 실패 시 local cache를 오염시키지 않는다`() = runTest {
        val failingCache = LettuceSuspendNearCache(
            redisClient = resp3Client,
            codec = failingValueCodec(),
            config = LettuceNearCacheConfig(cacheName = "fail-putall-susp-" + Base58.randomString(6))
        )

        failingCache.use { c ->
            assertWriteFailure {
                c.putAll(mapOf("k1" to "v1", "k2" to "v2"))
            }
            c.localCacheSize() shouldBeEqualTo 0L
        }
    }

    // ---- putIfAbsent TTL 원자성 ----

    @Test
    fun `putIfAbsent - TTL이 있는 경우 단일 명령으로 TTL까지 함께 저장한다`() = runTest {
        val ttlMs = 5_000L
        val ttlCacheName = "pia-ttl-susp-" + Base58.randomString(6)
        val ttlCache = LettuceSuspendNearCache(
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

    @Test
    fun `putIfAbsent - SuspendedJobTester 경쟁 상황에서도 단 한 번만 저장된다`() = runTest {
        val key = "suspend-contended-put-if-absent"
        val winnerCount = AtomicInteger(0)
        val observedValues = Collections.synchronizedList(mutableListOf<String>())
        val candidates = (1..8).map { "value-$it" }

        SuspendedJobTester()
            .workers(candidates.size)
            .rounds(1)
            .addAll(
                candidates.map { candidate ->
                    suspend {
                        val existing = cache.putIfAbsent(key, candidate)
                        if (existing == null) {
                            winnerCount.incrementAndGet()
                        } else {
                            observedValues += existing
                        }
                    }
                }
            )
            .run()

        winnerCount.get() shouldBeEqualTo 1
        val stored = cache.get(key).shouldNotBeNull()
        observedValues.forEach { it shouldBeEqualTo stored }
        directCommands.get("${cache.cacheName}:$key") shouldBeEqualTo stored
    }

    // ---- Millisecond TTL ----

    @Test
    fun `Redis TTL - millisecond 단위도 보존된다`() = runTest {
        val ttlMs = 5_000L
        val ttlCacheName = "ms-ttl-susp-" + Base58.randomString(6)
        val ttlCache = LettuceSuspendNearCache(
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
    fun `NearCacheConfig는 suspend 경로에서도 잘못된 설정값을 즉시 거부한다`() {
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

    private fun failingValueCodec(): RedisCodec<String, String> = object: RedisCodec<String, String> {
        private val delegate = StringCodec.UTF8

        override fun decodeKey(bytes: ByteBuffer): String = delegate.decodeKey(bytes)

        override fun decodeValue(bytes: ByteBuffer): String = delegate.decodeValue(bytes)

        override fun encodeKey(key: String): ByteBuffer = delegate.encodeKey(key)

        override fun encodeValue(value: String): ByteBuffer =
            throw IllegalStateException("encode failure for test")
    }

    private suspend fun assertWriteFailure(block: suspend () -> Unit) {
        val thrown = runCatching { block() }.exceptionOrNull()
        thrown.shouldNotBeNull()
        generateSequence(thrown) { it.cause }
            .any { it is IllegalStateException && it.message == "encode failure for test" }
            .shouldBeTrue()
    }

}
