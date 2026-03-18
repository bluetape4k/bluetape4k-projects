package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import javax.cache.expiry.EternalExpiryPolicy
import kotlin.time.Duration.Companion.seconds

/**
 * [ResilientNearCache] (JCache 기반 back cache) 동기(Blocking) 구현 테스트.
 *
 * write-behind + retry + graceful degradation 패턴을 검증한다.
 * back cache 반영은 비동기이므로 awaitility로 폴링한다.
 */
class ResilientNearCacheTest {

    companion object: KLogging() {
        const val REPEAT_SIZE = 3

        private fun randomKey(): String = TimebasedUuid.Epoch.nextIdAsString()
    }

    private val backCache = JCaching.Caffeine.getOrCreate<String, String>(
        name = "resilient-near-back-" + randomKey(),
        configuration = jcacheConfiguration {
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
        }
    )

    private lateinit var cache: ResilientNearCache<String, String>

    @BeforeEach
    fun createCache() {
        cache = ResilientNearCache(
            backCache = backCache,
            config = ResilientNearCacheConfig(
                retryMaxAttempts = 2,
                retryWaitDuration = java.time.Duration.ofMillis(100),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        runCatching { cache.close() }
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
    fun `put - write-behind - 잠시 후 back cache에도 반영됨`() {
        cache.put("wb-key", "wb-val")
        // front cache 즉시 확인
        cache.get("wb-key") shouldBeEqualTo "wb-val"
        // back cache는 write-behind로 비동기 반영 → awaitility 폴링
        await atMost 5.seconds until { backCache.get("wb-key") != null }
        backCache.get("wb-key") shouldBeEqualTo "wb-val"
    }

    @Test
    fun `get - front miss 시 back cache에서 읽어 front populate`() {
        backCache.put("remote-key", "remote-val")
        cache.get("remote-key") shouldBeEqualTo "remote-val"
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
    fun `remove - front 즉시 삭제, back write-behind`() {
        backCache.put("rm-key", "rm-val")
        cache.get("rm-key") shouldBeEqualTo "rm-val"

        cache.remove("rm-key")
        cache.get("rm-key").shouldBeNull()

        // back cache에서도 삭제되길 대기
        await atMost 5.seconds until { backCache.get("rm-key") == null }
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
    fun `containsKey - 키 존재 여부 확인`() {
        cache.put("keyX", "valX")
        cache.containsKey("keyX").shouldBeTrue()
        cache.containsKey("nonexistent").shouldBeFalse()
        cache.remove("keyX")
        cache.containsKey("keyX").shouldBeFalse()
    }

    @Test
    fun `putIfAbsent - 캐시 값 없으면 추가, 있으면 기존 값 반환`() {
        cache.putIfAbsent("key", "first").shouldBeNull()
        cache.get("key") shouldBeEqualTo "first"
        cache.putIfAbsent("key", "second") shouldBeEqualTo "first"
        cache.get("key") shouldBeEqualTo "first"
    }

    @Test
    fun `replace - 키가 존재할 때만 교체`() {
        cache.replace("noKey", "val").shouldBeFalse()
        cache.put("key", "old")

        // write-behind 완료 대기 (replace는 back cache 직접 호출)
        await atMost 5.seconds until { backCache.get("key") != null }

        cache.replace("key", "new").shouldBeTrue()
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `replace(key, oldValue, newValue) - 값이 일치할 때만 교체`() {
        cache.put("k", "old")
        await atMost 5.seconds until { backCache.get("k") != null }

        cache.replace("k", "wrong", "new").shouldBeFalse()
        cache.replace("k", "old", "new").shouldBeTrue()
        cache.get("k") shouldBeEqualTo "new"
    }

    @Test
    fun `getAndRemove - 캐시 값 조회 및 삭제`() {
        cache.put("key", "value")
        cache.getAndRemove("key") shouldBeEqualTo "value"
        cache.get("key").shouldBeNull()
        cache.getAndRemove("key").shouldBeNull()
    }

    @Test
    fun `getAndReplace - 캐시 값 조회 및 교체`() {
        cache.getAndReplace("missing", "val").shouldBeNull()
        cache.put("key", "old")

        await atMost 5.seconds until { backCache.get("key") != null }

        cache.getAndReplace("key", "new") shouldBeEqualTo "old"
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `clearLocal - 로컬만 초기화, back cache 유지`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearLocal()
        cache.localCacheSize() shouldBeEqualTo 0L

        await atMost 3.seconds until { backCache.get("k1") != null }

        // back cache에서 읽어와서 front에 populate
        cache.containsKey("k1").shouldBeTrue()
    }

    @Test
    fun `clearAll - write-behind - 잠시 후 back cache도 초기화`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")

        await atMost 5.seconds until { backCache.get("k1") != null }

        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L

        await atMost 5.seconds until { backCache.get("k1") == null }
    }

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = ResilientNearCache(backCache = backCache)
        c.close()
        c.close()
        c.isClosed.shouldBeTrue()
    }
}
