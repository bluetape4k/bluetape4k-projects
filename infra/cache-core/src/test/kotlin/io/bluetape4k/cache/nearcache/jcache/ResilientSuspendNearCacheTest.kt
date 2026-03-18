package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [ResilientSuspendNearCache] (SuspendCache 기반 back cache) Coroutine(Suspend) 구현 테스트.
 *
 * write-behind + retry + graceful degradation 패턴을 검증한다.
 * [runSuspendIO]를 사용해 IO Dispatcher에서 실제 시간으로 실행하여 write-behind consumer를 검증한다.
 */
class ResilientSuspendNearCacheTest {

    companion object: KLogging() {
        const val REPEAT_SIZE = 3
        private fun randomKey(): String = TimebasedUuid.Epoch.nextIdAsString()
    }

    private val backCache = CaffeineSuspendCache<String, String> {
        maximumSize(10_000)
        expireAfterWrite(Duration.ofMinutes(30))
    }

    private lateinit var cache: ResilientSuspendNearCache<String, String>

    @BeforeEach
    fun createCache() {
        cache = ResilientSuspendNearCache(
            backCache = backCache,
            config = ResilientNearCacheConfig(
                retryMaxAttempts = 2,
                retryWaitDuration = Duration.ofMillis(100),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        runCatching { cache.close() }
    }

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() = runSuspendIO {
        cache.get("missing-key").shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - front cache 즉시 반영`() = runSuspendIO {
        cache.put("key1", "value1")
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - write-behind - 잠시 후 back cache에도 반영됨`() = runSuspendIO {
        cache.put("wb-key", "wb-val")
        cache.get("wb-key") shouldBeEqualTo "wb-val"

        await atMost 5.seconds untilSuspending { backCache.get("wb-key") == "wb-val" }
        backCache.get("wb-key") shouldBeEqualTo "wb-val"
    }

    @Test
    fun `get - front miss 시 back cache에서 읽어 front populate`() = runSuspendIO {
        backCache.put("remote-key", "remote-val")
        cache.get("remote-key") shouldBeEqualTo "remote-val"
    }

    @Test
    fun `putAll and getAll`() = runSuspendIO {
        val data = mapOf("a" to "1", "b" to "2", "c" to "3")
        cache.putAll(data)
        val result = cache.getAll(setOf("a", "b", "c", "x"))
        result["a"] shouldBeEqualTo "1"
        result["b"] shouldBeEqualTo "2"
        result["c"] shouldBeEqualTo "3"
        result["x"].shouldBeNull()
    }

    @Test
    fun `remove - front 즉시 삭제, back write-behind`() = runSuspendIO {
        backCache.put("rm-key", "rm-val")
        cache.get("rm-key") shouldBeEqualTo "rm-val"

        cache.remove("rm-key")
        cache.get("rm-key").shouldBeNull()

        await atMost 5.seconds untilSuspending { backCache.get("rm-key") == null }
        backCache.get("rm-key").shouldBeNull()
    }

    @Test
    fun `removeAll - 여러 키 삭제`() = runSuspendIO {
        cache.putAll(mapOf("a" to "1", "b" to "2", "c" to "3"))
        cache.removeAll(setOf("a", "b"))
        cache.get("a").shouldBeNull()
        cache.get("b").shouldBeNull()
        cache.get("c") shouldBeEqualTo "3"
    }

    @Test
    fun `containsKey - 키 존재 여부 확인`() = runSuspendIO {
        cache.put("keyX", "valX")
        cache.containsKey("keyX").shouldBeTrue()
        cache.containsKey("nonexistent").shouldBeFalse()
        cache.remove("keyX")
        cache.containsKey("keyX").shouldBeFalse()
    }

    @Test
    fun `putIfAbsent - 캐시 값 없으면 추가, 있으면 기존 값 반환`() = runSuspendIO {
        cache.putIfAbsent("key", "first").shouldBeNull()
        cache.get("key") shouldBeEqualTo "first"
        cache.putIfAbsent("key", "second") shouldBeEqualTo "first"
        cache.get("key") shouldBeEqualTo "first"
    }

    @Test
    fun `replace - 키가 존재할 때만 교체`() = runSuspendIO {
        cache.replace("noKey", "val").shouldBeFalse()
        cache.put("key", "old")

        // replace는 back cache를 직접 호출 → write-behind 완료 대기
        await atMost 5.seconds untilSuspending { backCache.get("key") == "old" }

        cache.replace("key", "new").shouldBeTrue()
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `replace(key, oldValue, newValue) - 값이 일치할 때만 교체`() = runSuspendIO {
        cache.put("k", "old")
        await atMost 5.seconds untilSuspending { backCache.get("k") == "old" }

        cache.replace("k", "wrong", "new").shouldBeFalse()
        cache.replace("k", "old", "new").shouldBeTrue()
        cache.get("k") shouldBeEqualTo "new"
    }

    @Test
    fun `getAndRemove - 캐시 값 조회 및 삭제`() = runSuspendIO {
        cache.put("key", "value")
        cache.getAndRemove("key") shouldBeEqualTo "value"
        cache.get("key").shouldBeNull()
        cache.getAndRemove("key").shouldBeNull()
    }

    @Test
    fun `getAndReplace - 캐시 값 조회 및 교체`() = runSuspendIO {
        cache.getAndReplace("missing", "val").shouldBeNull()

        cache.put("key", "old")
        await atMost 5.seconds untilSuspending { backCache.get("key") == "old" }

        cache.getAndReplace("key", "new") shouldBeEqualTo "old"
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `clearLocal - 로컬만 초기화`() = runSuspendIO {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearLocal()
        cache.localCacheSize() shouldBeEqualTo 0L
    }

    @Test
    fun `clearAll - write-behind - 잠시 후 back cache도 초기화`() = runSuspendIO {
        cache.put("k1", "v1")
        cache.put("k2", "v2")

        await atMost 5.seconds untilSuspending { backCache.get("k1") == "v1" }
        backCache.get("k1") shouldBeEqualTo "v1"

        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L

        await atMost 5.seconds untilSuspending { backCache.get("k1") == null }
        backCache.get("k1").shouldBeNull()
    }

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = ResilientSuspendNearCache(backCache = backCache)
        c.close()
        c.close()
        c.isClosed.shouldBeTrue()
    }
}
