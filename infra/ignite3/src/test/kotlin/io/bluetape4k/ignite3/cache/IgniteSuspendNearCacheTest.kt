package io.bluetape4k.ignite3.cache

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

/**
 * [IgniteSuspendNearCache]의 suspend CRUD 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 3.x 서버를 실행하고, Caffeine + Ignite 3.x KeyValueView
 * 2-Tier Suspend NearCache의 비동기 캐싱 동작을 검증합니다.
 * [IgniteNearCacheManager]가 테이블을 자동으로 생성하므로 별도의 DDL이 필요하지 않습니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteSuspendNearCacheTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val TABLE_NAME = "TEST_SUSPEND_CACHE"
    }

    private lateinit var nearCache: IgniteSuspendNearCache<Long, String>

    @BeforeEach
    fun setup() {
        val config = IgniteNearCacheConfig(tableName = TABLE_NAME)
        // nearCacheManager가 테이블을 자동 생성하므로 @BeforeAll DDL 불필요
        nearCache = igniteClient.nearCacheManager().suspendNearCache<Long, String>(config)
        nearCache.clearFrontCache()
        igniteClient.sql().execute(null, "DELETE FROM $TABLE_NAME").close()
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() = runTest(timeout = 30.seconds) {
        val result = nearCache.get(9999L)
        result.shouldBeNull()
    }

    @Test
    fun `put으로 저장 후 get으로 조회 가능`() = runTest(timeout = 30.seconds) {
        nearCache.put(1L, "hello")

        val result = nearCache.get(1L)
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `Front Cache에 캐시된 값이 반환됨`() = runTest(timeout = 30.seconds) {
        nearCache.put(2L, "cached")

        // Front Cache에서 조회됨
        val result = nearCache.get(2L)
        result shouldBeEqualTo "cached"
    }

    @Test
    fun `Front Cache 초기화 후 Back Cache에서 조회`() = runTest(timeout = 30.seconds) {
        nearCache.put(3L, "back-cache-val")
        nearCache.clearFrontCache()

        // Front Cache 미스 → Ignite 3.x에서 조회
        val result = nearCache.get(3L)
        result shouldBeEqualTo "back-cache-val"
    }

    @Test
    fun `putAll로 여러 항목 일괄 저장 후 getAll로 조회`() = runTest(timeout = 30.seconds) {
        val entries = mapOf(10L to "ten", 20L to "twenty", 30L to "thirty")
        nearCache.putAll(entries)

        val result = nearCache.getAll(setOf(10L, 20L, 30L))
        result.size shouldBeEqualTo 3
        result[10L] shouldBeEqualTo "ten"
        result[20L] shouldBeEqualTo "twenty"
        result[30L] shouldBeEqualTo "thirty"
    }

    @Test
    fun `getAll에서 Front Cache 미스 시 Back Cache에서 일괄 조회`() = runTest(timeout = 30.seconds) {
        nearCache.put(40L, "forty")
        nearCache.put(50L, "fifty")
        nearCache.clearFrontCache()

        val result = nearCache.getAll(setOf(40L, 50L, 9999L))
        result.size shouldBeEqualTo 2
        result[40L] shouldBeEqualTo "forty"
        result[50L] shouldBeEqualTo "fifty"
    }

    @Test
    fun `remove로 Front Cache와 Back Cache에서 항목 삭제`() = runTest(timeout = 30.seconds) {
        nearCache.put(60L, "sixty")

        val removed = nearCache.remove(60L)
        removed.shouldBeTrue()

        nearCache.get(60L).shouldBeNull()
    }

    @Test
    fun `존재하지 않는 키를 remove하면 false 반환`() = runTest(timeout = 30.seconds) {
        val removed = nearCache.remove(9998L)
        removed.shouldBeFalse()
    }

    @Test
    fun `containsKey로 키 존재 여부 확인`() = runTest(timeout = 30.seconds) {
        nearCache.put(100L, "hundred")

        nearCache.containsKey(100L).shouldBeTrue()
        nearCache.containsKey(9997L).shouldBeFalse()
    }

    @Test
    fun `containsKey는 Front Cache 초기화 후 Back Cache에서도 확인`() = runTest(timeout = 30.seconds) {
        nearCache.put(110L, "one-ten")
        nearCache.clearFrontCache()

        // Front Cache 없이 Back Cache에서 확인
        nearCache.containsKey(110L).shouldBeTrue()
    }
}
