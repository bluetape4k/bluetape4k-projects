package io.bluetape4k.hazelcast.cache.coroutines

import io.bluetape4k.hazelcast.AbstractHazelcastTest
import io.bluetape4k.hazelcast.cache.HazelcastNearCacheConfig
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * [HazelcastSuspendNearCache]의 suspend CRUD 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Hazelcast 서버를 실행하고, Caffeine + Hazelcast IMap 2-Tier
 * Suspend NearCache의 비동기 캐싱 동작을 검증합니다.
 */
class HazelcastSuspendNearCacheTest: AbstractHazelcastTest() {

    companion object: KLogging() {
        private const val MAP_NAME = "test-suspend-near-cache-map"
    }

    private lateinit var nearCache: HazelcastSuspendNearCache<String, String>

    @BeforeEach
    fun setup() = runBlocking {
        val config = HazelcastNearCacheConfig(cacheName = MAP_NAME)
        nearCache = HazelcastSuspendNearCache(hazelcastClient.getMap(MAP_NAME), config)
        nearCache.clearAll()
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() = runTest(timeout = 30.seconds) {
        val result = nearCache.get("non-existent-key")
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
        nearCache.clear()  // front cache만 초기화

        // Front Cache 미스 → Back Cache(IMap)에서 조회
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
        nearCache.clear()  // front cache만 초기화

        val result = nearCache.getAll(setOf("x", "y", "missing"))
        result.size shouldBeEqualTo 2
        result["x"] shouldBeEqualTo "xval"
        result["y"] shouldBeEqualTo "yval"
    }

    @Test
    fun `remove로 Front Cache와 Back Cache에서 항목 삭제`() = runTest(timeout = 30.seconds) {
        nearCache.put("del-key", "del-value")

        nearCache.remove("del-key")

        nearCache.get("del-key").shouldBeNull()
    }

    @Test
    fun `존재하지 않는 키를 remove해도 예외 없이 처리`() = runTest(timeout = 30.seconds) {
        // 예외 없이 처리되어야 함
        nearCache.remove("no-such-key")
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
        nearCache.clear()  // front cache만 초기화

        // Front Cache 없이 Back Cache에서 확인
        nearCache.containsKey("front-only").shouldBeTrue()
    }

    @Test
    fun `clearAll로 Front Cache와 Back Cache 모두 초기화`() = runTest(timeout = 30.seconds) {
        nearCache.put("c1", "v1")
        nearCache.put("c2", "v2")

        nearCache.clearAll()

        nearCache.get("c1").shouldBeNull()
        nearCache.get("c2").shouldBeNull()
    }

    @Test
    fun `clear는 Front Cache만 초기화하고 Back Cache는 유지`() = runTest(timeout = 30.seconds) {
        nearCache.put("p1", "val1")
        nearCache.clear()  // front cache만 초기화

        // Back Cache에서 조회 가능해야 함
        val result = nearCache.get("p1")
        result shouldBeEqualTo "val1"
    }

    @Test
    fun `size가 Back Cache 항목 수를 반환`() = runTest(timeout = 30.seconds) {
        nearCache.clearAll()
        nearCache.put("s1", "v1")
        nearCache.put("s2", "v2")

        nearCache.size shouldBeEqualTo 2
    }

    @Test
    fun `name이 맵 이름을 반환`() = runTest(timeout = 30.seconds) {
        nearCache.name shouldBeEqualTo MAP_NAME
    }
}
