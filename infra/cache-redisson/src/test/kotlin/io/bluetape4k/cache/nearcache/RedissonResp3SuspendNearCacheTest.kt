package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.time.Duration

class RedissonResp3SuspendNearCacheTest : AbstractRedissonResp3NearCacheTest() {

    companion object : KLogging()

    private lateinit var cache: RedissonResp3SuspendNearCache<String>

    @BeforeEach
    fun createCache() {
        if (::cache.isInitialized) cache.close()
        cache = RedissonResp3SuspendNearCache(
            redisson = redisson,
            redisClient = resp3Client,
            config = RedissonResp3NearCacheConfig(cacheName = "test-resp3-suspend-cache"),
        )
    }

    @AfterEach
    fun tearDown() {
        if (::cache.isInitialized) cache.close()
    }

    // ---- 기본 CRUD ----

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() = runTest {
        cache.get("missing-key").shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - write-through`() = runTest {
        cache.put("key1", "value1")
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - Redis에도 반영됨`() = runTest {
        cache.put("k", "v")
        directCommands.get("${cache.cacheName}:k") shouldBeEqualTo "v"
    }

    @Test
    fun `get - front miss 시 Redis에서 읽어 populate`() = runTest {
        directCommands.set("${cache.cacheName}:remote-key", "remote-val")
        cache.get("remote-key") shouldBeEqualTo "remote-val"
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
        cache.containsKey("keyX") shouldBeEqualTo true
        cache.containsKey("nonexistent") shouldBeEqualTo false
        cache.remove("keyX")
        cache.containsKey("keyX") shouldBeEqualTo false
    }

    @Test
    fun `putIfAbsent - 캐시 값 없으면 추가, 있으면 기존 값 반환`() = runTest {
        cache.putIfAbsent("key", "first").shouldBeNull()
        cache.get("key") shouldBeEqualTo "first"
        cache.putIfAbsent("key", "second") shouldBeEqualTo "first"
        cache.get("key") shouldBeEqualTo "first"
    }

    @Test
    fun `replace - 키가 존재할 때만 교체`() = runTest {
        cache.replace("noKey", "val") shouldBeEqualTo false
        cache.put("key", "old")
        cache.replace("key", "new") shouldBeEqualTo true
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `replace(key, oldValue, newValue) - 값이 일치할 때만 교체`() = runTest {
        cache.put("k", "old")
        cache.replace("k", "wrong", "new") shouldBeEqualTo false
        cache.replace("k", "old", "new") shouldBeEqualTo true
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
        cache.clearLocal()
        cache.localSize() shouldBeEqualTo 0L
        // prefix key로 Redis 데이터 유지 확인
        directCommands.get("${cache.cacheName}:k1").shouldNotBeNull()
    }

    @Test
    fun `clearAll - 로컬 + Redis 초기화`() = runTest {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearAll()
        cache.localSize() shouldBeEqualTo 0L
        directCommands.get("${cache.cacheName}:k1").shouldBeNull()
        directCommands.get("${cache.cacheName}:k2").shouldBeNull()
    }

    @Test
    fun `clearAll - 다른 cacheName의 데이터는 유지됨`() = runTest {
        val otherCache = RedissonResp3SuspendNearCache(
            redisson = redisson,
            redisClient = resp3Client,
            config = RedissonResp3NearCacheConfig(cacheName = "other-resp3-suspend-cache-" + Base58.randomString(6)),
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
        val ttlCacheName = "ttl-resp3-suspend-test-" + Base58.randomString(6)
        val ttlCache = RedissonResp3SuspendNearCache(
            redisson = redisson,
            redisClient = resp3Client,
            config = RedissonResp3NearCacheConfig(
                cacheName = ttlCacheName,
                redisTtl = Duration.ofSeconds(10),
            ),
        )
        ttlCache.use { c ->
            c.put("ttl-key", "ttl-val")
            c.get("ttl-key") shouldBeEqualTo "ttl-val"
            val ttl = directCommands.ttl("${ttlCacheName}:ttl-key")
            (ttl > 0L).shouldBeTrue()
        }
    }

    // ---- backCacheSize ----

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
        val c = RedissonResp3SuspendNearCache(redisson, resp3Client)
        c.close()
        c.close()
    }
}
