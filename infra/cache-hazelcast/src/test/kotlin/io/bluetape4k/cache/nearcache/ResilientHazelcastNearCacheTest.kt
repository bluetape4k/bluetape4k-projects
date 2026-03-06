package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.TimeUnit

/**
 * Resilient Hazelcast IMap 기반 NearCache 동기(Blocking) 구현 테스트.
 *
 * write-behind + retry + graceful degradation 패턴을 검증한다.
 * IMap 반영은 비동기이므로 awaitility로 폴링한다.
 */
class ResilientHazelcastNearCacheTest : AbstractHazelcastNearCacheTest() {

    companion object : KLogging()

    private lateinit var cache: ResilientHazelcastNearCache<String>

    @BeforeEach
    fun createCache() {
        if (::cache.isInitialized) cache.close()
        cache = ResilientHazelcastNearCache(
            hazelcastInstance = hazelcastClient,
            config = ResilientHazelcastNearCacheConfig(
                base = HazelcastNearCacheConfig(cacheName = "resilient-near-cache-" + Base58.randomString(6)),
                retryMaxAttempts = 2,
            ),
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
        cache.get("missing-key").shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - front cache 즉시 반영`() {
        cache.put("key1", "value1")
        // front cache 즉시 확인
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - write-behind - 잠시 후 IMap에도 반영됨`() {
        cache.put("wb-key", "wb-val")
        // front cache 즉시 확인
        cache.get("wb-key") shouldBeEqualTo "wb-val"
        // IMap은 write-behind로 비동기 반영 → awaitility 폴링
        await.atMost(5, TimeUnit.SECONDS).until {
            cache.backCacheSize() == 1L
        }
        val imap = hazelcastClient.getMap<String, String>(cache.cacheName)
        imap["wb-key"] shouldBeEqualTo "wb-val"
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
    fun `remove - write-behind - 잠시 후 IMap에서도 삭제됨`() {
        // 먼저 IMap에 직접 추가
        val imap = hazelcastClient.getMap<String, String>(cache.cacheName)
        imap["rm-key"] = "rm-val"
        // front cache 읽어 populate
        cache.get("rm-key") shouldBeEqualTo "rm-val"

        // remove → front 즉시 삭제, IMap은 write-behind
        cache.remove("rm-key")
        cache.get("rm-key").shouldBeNull()

        // IMap에서도 삭제되길 기다림
        await.atMost(5, TimeUnit.SECONDS).until {
            imap["rm-key"] == null
        }
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
        // write-behind: IMap에 없는 키는 replace false
        cache.replace("noKey", "val") shouldBeEqualTo false
        cache.put("key", "old")
        // write-behind 완료 후 replace 호출
        await.atMost(5, TimeUnit.SECONDS).until { cache.backCacheSize() > 0L }
        cache.replace("key", "new") shouldBeEqualTo true
        cache.get("key") shouldBeEqualTo "new"
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
            localSize = { cache.localCacheSize() },
            containsKeyInBack = { cache.containsKey(it) },
        )
    }

    @Test
    fun `clearAll - write-behind - 잠시 후 IMap도 초기화`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        // write-behind로 IMap 반영 대기
        await.atMost(5, TimeUnit.SECONDS).until { cache.backCacheSize() == 2L }

        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L

        // IMap도 비워지길 대기
        await.atMost(5, TimeUnit.SECONDS).until { cache.backCacheSize() == 0L }
    }

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = ResilientHazelcastNearCache<String>(
            hazelcastInstance = hazelcastClient,
            config = ResilientHazelcastNearCacheConfig(
                base = HazelcastNearCacheConfig(cacheName = "resilient-close-" + Base58.randomString(6)),
            ),
        )
        c.close()
        c.close()
        c.isClosed.shouldBeTrue()
    }
}
