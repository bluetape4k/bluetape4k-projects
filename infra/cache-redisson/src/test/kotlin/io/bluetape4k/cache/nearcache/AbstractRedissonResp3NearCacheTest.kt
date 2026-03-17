package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.protocol.ProtocolVersion
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeEach
import org.redisson.api.RedissonClient
import kotlin.time.Duration.Companion.seconds

/**
 * [RedissonResp3NearCache] / [RedissonResp3SuspendNearCache] 공통 테스트 베이스.
 * Redis 7+ testcontainers를 사용하며, RESP3 CLIENT TRACKING 기능을 검증한다.
 */
abstract class AbstractRedissonResp3NearCacheTest {

    companion object: KLogging() {
        const val REPEAT_SIZE = 3

        val clientRESP3Protocol: ClientOptions = ClientOptions.builder()
            .protocolVersion(ProtocolVersion.RESP3)
            .build()

        /** Redisson 클라이언트 */
        val redisson: RedissonClient by lazy { RedisServers.redisson }

        /** RESP3 활성화된 RedisClient (Lettuce, tracking 전용) */
        val resp3Client: RedisClient by lazy {
            RedisServers.redisClient.also { client ->
                client.options = clientRESP3Protocol
            }
        }

        /** 검증용 직접 Redis 명령 (tracking 없음) */
        val directCommands: RedisCommands<String, String> by lazy {
            RedisServers.redisClient.connect<String, String>(LettuceBinaryCodecs.lz4Fory()).sync()
        }
    }

    @BeforeEach
    fun setup() {
        directCommands.flushdb()
    }

    // -----------------------------------------------------------------------------------------
    // 공통 테스트 헬퍼
    // -----------------------------------------------------------------------------------------

    protected fun verifyGetMiss(get: (String) -> String?) {
        get("missing-key").shouldBeNull()
    }

    protected fun verifyPutAndGet(put: (String, String) -> Unit, get: (String) -> String?) {
        put("key1", "value1")
        get("key1") shouldBeEqualTo "value1"
    }

    protected fun verifyRemove(
        put: (String, String) -> Unit,
        get: (String) -> String?,
        remove: (String) -> Unit,
    ) {
        put("key1", "value1")
        get("key1").shouldNotBeNull()
        remove("key1")
        get("key1").shouldBeNull()
    }

    protected fun verifyContainsKey(
        put: (String, String) -> Unit,
        containsKey: (String) -> Boolean,
        remove: (String) -> Unit,
    ) {
        put("keyX", "valX")
        containsKey("keyX").shouldBeTrue()
        containsKey("nonexistent").shouldBeFalse()
        remove("keyX")
        containsKey("keyX").shouldBeFalse()
    }

    protected fun verifyPutIfAbsent(
        putIfAbsent: (String, String) -> String?,
        get: (String) -> String?,
    ) {
        putIfAbsent("key", "first").shouldBeNull()           // 새로 저장 → null 반환
        get("key") shouldBeEqualTo "first"
        putIfAbsent("key", "second") shouldBeEqualTo "first" // 이미 존재 → 기존 값 반환
        get("key") shouldBeEqualTo "first"
    }

    protected fun verifyGetAll(
        putAll: (Map<String, String>) -> Unit,
        getAll: (Set<String>) -> Map<String, String>,
    ) {
        val data = mapOf("a" to "1", "b" to "2", "c" to "3")
        putAll(data)
        val result = getAll(setOf("a", "b", "c", "x"))
        result["a"] shouldBeEqualTo "1"
        result["b"] shouldBeEqualTo "2"
        result["c"] shouldBeEqualTo "3"
        result["x"].shouldBeNull()
    }

    protected fun verifyReplace(
        put: (String, String) -> Unit,
        replace: (String, String) -> Boolean,
        get: (String) -> String?,
    ) {
        replace("noKey", "val").shouldBeFalse()
        put("key", "old")
        replace("key", "new").shouldBeTrue()
        get("key") shouldBeEqualTo "new"
    }

    protected fun verifyGetAndRemove(
        put: (String, String) -> Unit,
        getAndRemove: (String) -> String?,
        get: (String) -> String?,
    ) {
        put("key", "value")
        getAndRemove("key") shouldBeEqualTo "value"
        get("key").shouldBeNull()
        getAndRemove("key").shouldBeNull()
    }

    protected fun verifyGetAndReplace(
        put: (String, String) -> Unit,
        getAndReplace: (String, String) -> String?,
        get: (String) -> String?,
    ) {
        getAndReplace("missing", "val").shouldBeNull()
        put("key", "old")
        getAndReplace("key", "new") shouldBeEqualTo "old"
        get("key") shouldBeEqualTo "new"
    }

    protected fun verifyRemoveAll(
        putAll: (Map<String, String>) -> Unit,
        removeAll: (Set<String>) -> Unit,
        get: (String) -> String?,
    ) {
        putAll(mapOf("a" to "1", "b" to "2", "c" to "3"))
        removeAll(setOf("a", "b"))
        get("a").shouldBeNull()
        get("b").shouldBeNull()
        get("c") shouldBeEqualTo "3"
    }

    protected fun verifyClearLocal(
        put: (String, String) -> Unit,
        clearLocal: () -> Unit,
        localSize: () -> Long,
        getFromRedis: (String) -> String?,
    ) {
        put("k1", "v1")
        put("k2", "v2")
        // local size는 Redisson 쓰기로 인한 invalidation으로 0일 수도 있으므로,
        // Redis에 데이터가 있음을 확인하는 것으로 대체
        clearLocal()
        localSize() shouldBeEqualTo 0L

        await atMost 3.seconds until { getFromRedis("k1") != null }

        // Redis에 데이터 유지 확인 (prefix key로)
        getFromRedis("k1").shouldNotBeNull()
    }
}
