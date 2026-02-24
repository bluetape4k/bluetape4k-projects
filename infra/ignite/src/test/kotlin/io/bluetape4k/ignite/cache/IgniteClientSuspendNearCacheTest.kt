package io.bluetape4k.ignite.cache

import io.bluetape4k.ignite.AbstractIgniteTest
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

/**
 * [IgniteClientSuspendNearCache]의 suspend CRUD 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 2.x 서버를 실행하고, 씬 클라이언트로 연결하여
 * Caffeine + ClientCache 2-Tier Suspend NearCache를 테스트합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteClientSuspendNearCacheTest: AbstractIgniteTest() {

    companion object: KLogging() {
        private const val CACHE_NAME = "TEST_SUSPEND_NEAR_CACHE"
    }

    private lateinit var nearCache: IgniteClientSuspendNearCache<String, String>

    @BeforeEach
    fun setup() {
        val config = IgniteNearCacheConfig(cacheName = CACHE_NAME)
        nearCache = IgniteClientSuspendNearCache(igniteClient, config)
        nearCache.clearFrontCache()
        nearCache.backCache.clear()
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() = runTest(timeout = 30.seconds) {
        val result = nearCache.get("non-existent")
        result.shouldBeNull()
    }

    @Test
    fun `put으로 저장 후 get으로 조회 가능`() = runTest(timeout = 30.seconds) {
        nearCache.put("key1", "value1")

        val result = nearCache.get("key1")
        result shouldBeEqualTo "value1"
    }

    @Test
    fun `Front Cache에 캐시된 값이 반환됨`() = runTest(timeout = 30.seconds) {
        nearCache.put("fc-key", "fc-value")

        // Front Cache에서 조회됨
        val result = nearCache.get("fc-key")
        result shouldBeEqualTo "fc-value"
    }

    @Test
    fun `Front Cache 초기화 후 Back Cache에서 조회`() = runTest(timeout = 30.seconds) {
        nearCache.put("bc-key", "bc-value")
        nearCache.clearFrontCache()

        // Front Cache 미스 → Back Cache(Ignite 2.x)에서 조회
        val result = nearCache.get("bc-key")
        result shouldBeEqualTo "bc-value"
    }

    @Test
    fun `putAll로 여러 항목 일괄 저장 후 getAll로 조회`() = runTest(timeout = 30.seconds) {
        val entries = mapOf("a" to "alpha", "b" to "beta", "c" to "gamma")
        nearCache.putAll(entries)

        val result = nearCache.getAll(setOf("a", "b", "c"))
        result.size shouldBeEqualTo 3
        result["a"] shouldBeEqualTo "alpha"
        result["b"] shouldBeEqualTo "beta"
        result["c"] shouldBeEqualTo "gamma"
    }

    @Test
    fun `getAll에서 Front Cache 미스 시 Back Cache에서 일괄 조회`() = runTest(timeout = 30.seconds) {
        nearCache.put("x", "xval")
        nearCache.put("y", "yval")
        nearCache.clearFrontCache()

        val result = nearCache.getAll(setOf("x", "y", "missing"))
        result.size shouldBeEqualTo 2
        result["x"] shouldBeEqualTo "xval"
        result["y"] shouldBeEqualTo "yval"
    }

    @Test
    fun `remove로 Front Cache와 Back Cache에서 항목 삭제`() = runTest(timeout = 30.seconds) {
        nearCache.put("del-key", "del-value")

        val removed = nearCache.remove("del-key")
        removed.shouldBeTrue()

        nearCache.get("del-key").shouldBeNull()
    }

    @Test
    fun `존재하지 않는 키를 remove하면 false 반환`() = runTest(timeout = 30.seconds) {
        val removed = nearCache.remove("no-such-key")
        removed.shouldBeFalse()
    }

    @Test
    fun `containsKey로 키 존재 여부 확인`() = runTest(timeout = 30.seconds) {
        nearCache.put("ck-key", "ck-value")

        nearCache.containsKey("ck-key").shouldBeTrue()
        nearCache.containsKey("ck-missing").shouldBeFalse()
    }

    @Test
    fun `containsKey는 Front Cache 초기화 후 Back Cache에서도 확인`() = runTest(timeout = 30.seconds) {
        nearCache.put("front-only", "value")
        nearCache.clearFrontCache()

        // Front Cache 없이 Back Cache에서 확인
        nearCache.containsKey("front-only").shouldBeTrue()
    }

    @Test
    fun `name이 캐시 이름을 반환`() = runTest(timeout = 30.seconds) {
        nearCache.shouldNotBeNull()
        nearCache.name shouldBeEqualTo CACHE_NAME
    }
}
