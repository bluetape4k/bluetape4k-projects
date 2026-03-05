package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.time.Duration

class RedissonResp3NearCacheTest : AbstractRedissonResp3NearCacheTest() {

    companion object : KLogging()

    private lateinit var cache: RedissonResp3NearCache<String>

    @BeforeAll
    fun createCache() {
        if (::cache.isInitialized) cache.close()
        cache = RedissonResp3NearCache(
            redisson = redisson,
            redisClient = resp3Client,
            config = RedissonResp3NearCacheConfig(cacheName = "test-resp3-near-cache-" + Base58.randomString(6)),
        )
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
    fun `put and get - write-through`() {
        verifyPutAndGet(
            put = { k, v -> cache.put(k, v) },
            get = { cache.get(it) },
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

        // Redisson 이 RESP3가 아닐 경우에는 tracking이 활성화 안 될 수 있으나,
        // 값을 가져오는 동작은 정상이어야 함
        cache.get("remote-key") shouldBeEqualTo "remote-val"
    }

    @Test
    fun `putAll and getAll`() {
        verifyGetAll(
            putAll = { cache.putAll(it) },
            getAll = { cache.getAll(it) },
        )
    }

    @Test
    fun `remove - front + Redis 삭제`() {
        verifyRemove(
            put = { k, v -> cache.put(k, v) },
            get = { cache.get(it) },
            remove = { cache.remove(it) },
        )
    }

    @Test
    fun `removeAll - 여러 키 삭제`() {
        verifyRemoveAll(
            putAll = { cache.putAll(it) },
            removeAll = { cache.removeAll(it) },
            get = { cache.get(it) },
        )
    }

    @Test
    fun `containsKey - 키 존재 여부 확인`() {
        verifyContainsKey(
            put = { k, v -> cache.put(k, v) },
            containsKey = { cache.containsKey(it) },
            remove = { cache.remove(it) },
        )
    }

    @Test
    fun `putIfAbsent - 캐시 값 없으면 추가, 있으면 기존 값 반환`() {
        verifyPutIfAbsent(
            putIfAbsent = { k, v -> cache.putIfAbsent(k, v) },
            get = { cache.get(it) },
        )
    }

    @Test
    fun `replace - 키가 존재할 때만 교체`() {
        verifyReplace(
            put = { k, v -> cache.put(k, v) },
            replace = { k, v -> cache.replace(k, v) },
            get = { cache.get(it) },
        )
    }

    @Test
    fun `replace(key, oldValue, newValue) - 값이 일치할 때만 교체`() {
        cache.put("k", "old")
        cache.replace("k", "wrong", "new") shouldBeEqualTo false
        cache.replace("k", "old", "new") shouldBeEqualTo true
        cache.get("k") shouldBeEqualTo "new"
    }

    @Test
    fun `getAndRemove - 캐시 값 조회 및 삭제`() {
        verifyGetAndRemove(
            put = { k, v -> cache.put(k, v) },
            getAndRemove = { cache.getAndRemove(it) },
            get = { cache.get(it) },
        )
    }

    @Test
    fun `getAndReplace - 캐시 값 조회 및 교체`() {
        verifyGetAndReplace(
            put = { k, v -> cache.put(k, v) },
            getAndReplace = { k, v -> cache.getAndReplace(k, v) },
            get = { cache.get(it) },
        )
    }

    // ---- clearLocal / clearAll ----

    @Test
    fun `clearLocal - 로컬만 초기화, Redis 유지`() {
        verifyClearLocal(
            put = { k, v -> cache.put(k, v) },
            clearLocal = { cache.clearLocal() },
            localSize = { cache.localCacheSize() },
            getFromRedis = { directCommands.get("${cache.cacheName}:$it") },
        )
    }

    @Test
    fun `clearAll - 로컬 + Redis 초기화`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L
        directCommands.get("${cache.cacheName}:k1").shouldBeNull()
        directCommands.get("${cache.cacheName}:k2").shouldBeNull()
    }

    @Test
    fun `clearAll - 다른 cacheName의 데이터는 유지됨`() {
        val otherCache = RedissonResp3NearCache(
            redisson = redisson,
            redisClient = resp3Client,
            config = RedissonResp3NearCacheConfig(cacheName = "other-resp3-cache-" + Base58.randomString(6)),
        )
        otherCache.use { other ->
            cache.put("shared-key", "from-main-cache")
            other.put("shared-key", "from-other-cache")

            cache.clearAll()

            cache.get("shared-key").shouldBeNull()
            directCommands.get("${cache.cacheName}:shared-key").shouldBeNull()

            other.get("shared-key") shouldBeEqualTo "from-other-cache"
            directCommands.get("${other.cacheName}:shared-key") shouldBeEqualTo "from-other-cache"
        }
    }

    // ---- TTL ----

    @Test
    fun `Redis TTL - TTL이 있는 캐시 설정`() {
        val ttlCacheName = "ttl-resp3-test-" + Base58.randomString(6)
        val ttlCache = RedissonResp3NearCache(
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
    fun `backCacheSize - cacheName에 속한 Redis key 개수`() {
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
        val c = RedissonResp3NearCache(redisson, resp3Client)
        c.close()
        c.close()
    }
}
