package io.bluetape4k.ignite.cache

import io.bluetape4k.ignite.AbstractIgniteTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [IgniteClientNearCache]의 CRUD 및 Front Cache 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 2.x 서버를 실행하고, 씬 클라이언트로 연결하여 테스트합니다.
 */
class IgniteClientNearCacheTest: AbstractIgniteTest() {

    companion object: KLogging() {
        private const val CACHE_NAME = "TEST_NEAR_CACHE"
    }

    private lateinit var nearCache: IgniteClientNearCache<String, String>

    @BeforeEach
    fun setup() {
        val config = IgniteNearCacheConfig(cacheName = CACHE_NAME)
        nearCache = IgniteClientNearCache(igniteClient, config)
        // 기존 데이터 초기화
        nearCache.clearFrontCache()
        nearCache.backCache.clear()
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() {
        val result = nearCache.get("non-existent")
        result.shouldBeNull()
    }

    @Test
    fun `put으로 저장 후 get으로 조회 가능`() {
        nearCache.put("key1", "value1")

        val result = nearCache.get("key1")
        result shouldBeEqualTo "value1"
    }

    @Test
    fun `Front Cache에 캐시된 값이 반환됨`() {
        nearCache.put("fc-key", "fc-value")

        // Front Cache에서 조회 (Back Cache 접근 없이)
        val result = nearCache.get("fc-key")
        result shouldBeEqualTo "fc-value"
    }

    @Test
    fun `Front Cache 초기화 후 Back Cache에서 조회`() {
        nearCache.put("bc-key", "bc-value")
        nearCache.clearFrontCache()

        // Front Cache 미스 → Back Cache에서 조회
        val result = nearCache.get("bc-key")
        result shouldBeEqualTo "bc-value"
    }

    @Test
    fun `putAll로 여러 항목 일괄 저장 후 getAll로 조회`() {
        val entries = mapOf("a" to "alpha", "b" to "beta", "c" to "gamma")
        nearCache.putAll(entries)

        val result = nearCache.getAll(setOf("a", "b", "c"))
        result.size shouldBeEqualTo 3
        result["a"] shouldBeEqualTo "alpha"
        result["b"] shouldBeEqualTo "beta"
        result["c"] shouldBeEqualTo "gamma"
    }

    @Test
    fun `getAll에서 Front Cache 미스 시 Back Cache에서 일괄 조회`() {
        nearCache.put("x", "xval")
        nearCache.put("y", "yval")
        nearCache.clearFrontCache()

        val result = nearCache.getAll(setOf("x", "y", "missing"))
        result.size shouldBeEqualTo 2
        result["x"] shouldBeEqualTo "xval"
        result["y"] shouldBeEqualTo "yval"
    }

    @Test
    fun `remove로 Front Cache와 Back Cache에서 항목 삭제`() {
        nearCache.put("del-key", "del-value")

        val removed = nearCache.remove("del-key")
        removed.shouldBeTrue()

        nearCache.get("del-key").shouldBeNull()
    }

    @Test
    fun `존재하지 않는 키를 remove하면 false 반환`() {
        val removed = nearCache.remove("no-such-key")
        removed.shouldBeFalse()
    }

    @Test
    fun `removeAll로 여러 항목 일괄 삭제`() {
        nearCache.put("r1", "v1")
        nearCache.put("r2", "v2")
        nearCache.put("r3", "v3")

        nearCache.removeAll(setOf("r1", "r2"))

        nearCache.get("r1").shouldBeNull()
        nearCache.get("r2").shouldBeNull()
        nearCache.get("r3") shouldBeEqualTo "v3"
    }

    @Test
    fun `containsKey로 키 존재 여부 확인`() {
        nearCache.put("ck-key", "ck-value")

        nearCache.containsKey("ck-key").shouldBeTrue()
        nearCache.containsKey("ck-missing").shouldBeFalse()
    }

    @Test
    fun `containsKey는 Front Cache 또는 Back Cache 중 하나에 있으면 true 반환`() {
        nearCache.put("front-only", "value")
        nearCache.clearFrontCache()

        // Front Cache에 없지만 Back Cache에 있음
        nearCache.containsKey("front-only").shouldBeTrue()
    }

    @Test
    fun `name이 캐시 이름을 반환`() {
        nearCache.shouldNotBeNull()
        nearCache.name shouldBeEqualTo CACHE_NAME
    }
}
