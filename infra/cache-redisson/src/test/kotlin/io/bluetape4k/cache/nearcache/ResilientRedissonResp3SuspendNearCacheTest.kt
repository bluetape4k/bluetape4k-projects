package io.bluetape4k.cache.nearcache

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
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
import kotlin.time.Duration.Companion.seconds

/**
 * Resilient Redisson + Lettuce RESP3 하이브리드 NearCache Coroutine(Suspend) 구현 테스트.
 *
 * write-behind + retry + graceful degradation 패턴을 검증한다.
 * Redis 반영은 비동기이므로 awaitility로 폴링한다.
 */
class ResilientRedissonResp3SuspendNearCacheTest: AbstractRedissonResp3NearCacheTest() {

    companion object: KLogging()

    private lateinit var cache: ResilientRedissonResp3SuspendNearCache<String>

    @BeforeEach
    fun createCache() {
        if (::cache.isInitialized) cache.close()
        cache = ResilientRedissonResp3SuspendNearCache(
            redisson = redisson,
            redisClient = resp3Client,
            config = ResilientRedissonResp3NearCacheConfig(
                base = RedissonResp3NearCacheConfig(cacheName = "resilient-resp3-suspend-" + Base58.randomString(6)),
                retryMaxAttempts = 2,
            ),
        )
    }

    @AfterEach
    fun tearDown() = runTest {
        if (::cache.isInitialized) {
            runCatching { cache.close() }
        }
    }

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() = runTest {
        cache.get("missing-key").shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - front cache 즉시 반영`() = runTest {
        cache.put("key1", "value1")
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - write-behind - 잠시 후 Redis에도 반영됨`() = runTest(timeout = 10.seconds) {
        cache.put("wb-key", "wb-val")
        cache.get("wb-key") shouldBeEqualTo "wb-val"
        // Redis는 write-behind로 비동기 반영 → awaitility 폴링
        await.atMost(5, TimeUnit.SECONDS).untilNotNull {
            directCommands.get("${cache.cacheName}:wb-key")
        }
        directCommands.get("${cache.cacheName}:wb-key") shouldBeEqualTo "wb-val"
    }

    @Test
    fun `get - front miss 시 Redis에서 읽어 front populate`() = runTest {
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
    fun `remove - front 즉시 삭제, Redis write-behind`() = runTest(timeout = 10.seconds) {
        directCommands.set("${cache.cacheName}:rm-key", "rm-val")
        cache.get("rm-key") shouldBeEqualTo "rm-val"

        cache.remove("rm-key")
        cache.get("rm-key").shouldBeNull()

        await.atMost(5, TimeUnit.SECONDS).untilNull {
            directCommands.get("${cache.cacheName}:rm-key")
        }
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
    fun `putIfAbsent - 캐시 값 없으면 추가, 있으면 기존 값 반환`() = runTest {
        cache.putIfAbsent("key", "first").shouldBeNull()
        cache.get("key") shouldBeEqualTo "first"
        cache.putIfAbsent("key", "second") shouldBeEqualTo "first"
        cache.get("key") shouldBeEqualTo "first"
    }

    @Test
    fun `replace - 키가 존재할 때만 교체`() = runTest {
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

    @Test
    fun `clearLocal - 로컬만 초기화, Redis 유지`() = runTest {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearLocal()
        cache.localCacheSize() shouldBeEqualTo 0L
        // Redis 데이터는 write-behind로 반영될 수 있으므로 별도 확인
    }

    @Test
    fun `clearAll - write-behind - 잠시 후 Redis도 초기화`() = runTest(timeout = 10.seconds) {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        await.atMost(5, TimeUnit.SECONDS).until {
            directCommands.get("${cache.cacheName}:k1") != null
        }

        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L

        await.atMost(5, TimeUnit.SECONDS).untilNull {
            directCommands.get("${cache.cacheName}:k1")
        }
    }

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = ResilientRedissonResp3SuspendNearCache<String>(
            redisson = redisson,
            redisClient = resp3Client,
        )
        c.close()
        c.close()
        c.isClosed.shouldBeTrue()
    }

    /**
     * 여러 코루틴에서 동시에 put/get을 호출해도 예외 없이 처리되는지 검증한다.
     * write-behind 특성상 Redis 즉시 반영은 기대하지 않으며, front cache의 코루틴 동시성 안전성만 검증한다.
     */
    @Test
    fun `코루틴 동시 put-get - 충돌 없이 처리됨`() = runTest {
        SuspendedJobTester()
            .workers(16)
            .rounds(4)
            .add {
                val key = "sj-key-${(Math.random() * 8).toInt()}"
                val value = "sj-val-$key"
                cache.put(key, value)
                cache.get(key) // null 또는 다른 코루틴 값일 수 있으므로 결과 무시
            }
            .run()
    }
}
