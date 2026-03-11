package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.awaitility.kotlin.untilNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.TimeUnit

/**
 * Resilient Redisson + Lettuce RESP3 하이브리드 NearCache 동기(Blocking) 구현 테스트.
 *
 * write-behind + retry + graceful degradation 패턴을 검증한다.
 * Redis 반영은 비동기이므로 awaitility로 폴링한다.
 */
class ResilientRedissonResp3NearCacheTest: AbstractRedissonResp3NearCacheTest() {

    companion object: KLogging()

    private lateinit var cache: ResilientRedissonResp3NearCache<String>

    @BeforeEach
    fun createCache() {
        if (::cache.isInitialized) cache.close()
        cache = ResilientRedissonResp3NearCache(
            redisson = redisson,
            redisClient = resp3Client,
            config = ResilientRedissonResp3NearCacheConfig(
                base = RedissonResp3NearCacheConfig(cacheName = "resilient-resp3-cache-" + Base58.randomString(6)),
                retryMaxAttempts = 2,
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        if (::cache.isInitialized) {
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
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - write-behind - 잠시 후 Redis에도 반영됨`() {
        cache.put("wb-key", "wb-val")
        // front cache 즉시 확인
        cache.get("wb-key") shouldBeEqualTo "wb-val"
        // Redis는 write-behind로 비동기 반영 → awaitility 폴링
        await.atMost(5, TimeUnit.SECONDS).untilNotNull {
            directCommands.get("${cache.cacheName}:wb-key")
        }
        directCommands.get("${cache.cacheName}:wb-key") shouldBeEqualTo "wb-val"
    }

    @Test
    fun `get - front miss 시 Redis에서 읽어 front populate`() {
        directCommands.set("${cache.cacheName}:remote-key", "remote-val")
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
    fun `remove - front 즉시 삭제, Redis write-behind`() {
        directCommands.set("${cache.cacheName}:rm-key", "rm-val")
        cache.get("rm-key") shouldBeEqualTo "rm-val"

        cache.remove("rm-key")
        cache.get("rm-key").shouldBeNull()

        // Redis에서도 삭제되길 대기
        await.atMost(5, TimeUnit.SECONDS).untilNull {
            directCommands.get("${cache.cacheName}:rm-key")
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
    fun `clearLocal - 로컬만 초기화, Redis 유지`() {
        verifyClearLocal(
            put = { k, v -> cache.put(k, v) },
            clearLocal = { cache.clearLocal() },
            localSize = { cache.localCacheSize() },
            getFromRedis = { directCommands.get("${cache.cacheName}:$it") },
        )
    }

    @Test
    fun `clearAll - write-behind - 잠시 후 Redis도 초기화`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        // write-behind로 Redis 반영 대기
        await.atMost(5, TimeUnit.SECONDS).until {
            directCommands.get("${cache.cacheName}:k1") != null
        }

        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L

        // Redis도 비워지길 대기
        await.atMost(5, TimeUnit.SECONDS).untilNull {
            directCommands.get("${cache.cacheName}:k1")
        }
    }

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = ResilientRedissonResp3NearCache<String>(
            redisson = redisson,
            redisClient = resp3Client,
        )
        c.close()
        c.close()
        c.isClosed.shouldBeTrue()
    }

    /**
     * 여러 스레드에서 동시에 put/get을 호출해도 예외 없이 처리되는지 검증한다.
     * write-behind 특성상 Redis 즉시 반영은 기대하지 않으며, front cache의 동시성 안전성만 검증한다.
     */
    @Test
    fun `멀티스레드 동시 put-get - 충돌 없이 처리됨`() {
        MultithreadingTester()
            .workers(16)
            .rounds(4)
            .add {
                val key = "mt-key-${Thread.currentThread().threadId() % 8}"
                val value = "mt-val-${Thread.currentThread().threadId()}"
                cache.put(key, value)
                cache.get(key) // null 또는 다른 스레드 값일 수 있으므로 결과 무시
            }
            .run()
    }

    /**
     * Virtual Thread 기반 StructuredTaskScope 환경에서 put/get/remove를 혼합하여
     * 동시에 호출해도 예외 없이 처리되는지 검증한다.
     */
    @Test
    fun `StructuredTaskScope 동시 put-get-remove - 충돌 없이 처리됨`() {
        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                val key = "stc-key-${(Math.random() * 8).toInt()}"
                cache.put(key, "stc-val")
            }
            .add {
                val key = "stc-key-${(Math.random() * 8).toInt()}"
                cache.get(key) // null 허용
            }
            .add {
                val key = "stc-key-${(Math.random() * 8).toInt()}"
                cache.remove(key)
            }
            .run()
    }
}
