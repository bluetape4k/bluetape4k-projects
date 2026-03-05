package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58

/**
 * Hazelcast IMap 기반 NearCache 동기(Blocking) 구현 테스트.
 *
 * JCache 기반 테스트와 달리 리스너 직렬화 문제가 없다.
 */
class HazelcastNearCacheTest : AbstractHazelcastNearCacheTest() {

    companion object : KLogging()

    private lateinit var cache: HazelcastNearCache<String>

    @BeforeEach
    fun createCache() {
        if (::cache.isInitialized) cache.close()
        cache = HazelcastNearCache(
            hazelcastInstance = hazelcastClient,
            config = HazelcastNearCacheConfig(cacheName = "test-near-cache-" + Base58.randomString(6)),
        )
    }

    @AfterEach
    fun tearDown() {
        if (::cache.isInitialized) {
            runCatching { cache.clearAll() }
            runCatching { cache.close() }
        }
    }

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
    fun `put - IMap에도 반영됨`() {
        cache.put("k", "v")
        cache.backCacheSize() shouldBeEqualTo 1
    }

    @Test
    fun `get - front miss 시 IMap에서 읽어 front populate`() {
        val imap = hazelcastClient.getMap<String, String>(cache.cacheName)
        imap["remote-key"] = "remote-val"
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
    fun `remove - front + IMap 삭제`() {
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

    @Test
    fun `clearLocal - 로컬만 초기화, IMap 유지`() {
        verifyClearLocal(
            put = { k, v -> cache.put(k, v) },
            clearLocal = { cache.clearLocal() },
            localSize = { cache.localSize() },
            containsKeyInBack = { cache.containsKey(it) },
        )
    }

    @Test
    fun `clearAll - 로컬 + IMap 초기화`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearAll()
        cache.localSize() shouldBeEqualTo 0L
        cache.get("k1").shouldBeNull()
        cache.get("k2").shouldBeNull()
    }

    @Test
    fun `backCacheSize - IMap의 key 개수`() {
        cache.put("s1", "v1")
        cache.put("s2", "v2")
        cache.put("s3", "v3")
        cache.backCacheSize() shouldBeEqualTo 3
        cache.remove("s2")
        cache.backCacheSize() shouldBeEqualTo 2
    }

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = HazelcastNearCache<String>(
            hazelcastInstance = hazelcastClient,
            config = HazelcastNearCacheConfig(cacheName = "test-near-close-" + Base58.randomString(6)),
        )
        c.close()
        c.close()
        c.isClosed.shouldBeTrue()
    }
}
