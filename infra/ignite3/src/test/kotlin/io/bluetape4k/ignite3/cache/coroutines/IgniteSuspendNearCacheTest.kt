package io.bluetape4k.ignite3.cache.coroutines

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.ignite3.cache.igniteNearCacheConfig
import io.bluetape4k.ignite3.cache.nearCacheManager
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.toList
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
 * [IgniteSuspendNearCache] ([NearSuspendCache])의 suspend CRUD 동작을 검증하는 통합 테스트입니다.
 *
 * NearSuspendCache는 cache-aside 패턴으로 Front Cache miss 시 Back Cache에서 조회합니다.
 * Ignite 3.x 씬 클라이언트는 이벤트 리스너를 지원하지 않으므로,
 * 이 cache-aside 동작이 이벤트 전파의 대안으로 작동합니다.
 */
class IgniteSuspendNearCacheTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val TABLE_NAME = "TEST_SUSPEND_CACHE"
    }

    private lateinit var nearCache: IgniteSuspendNearCache<Long, String>

    @BeforeEach
    fun setup() {
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        nearCache = igniteClient.nearCacheManager().suspendNearCache(config)
        runBlocking { nearCache.clear() }
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
        val result = nearCache.get(2L)
        result shouldBeEqualTo "cached"
    }

    @Test
    fun `Front Cache 초기화 후 Back Cache에서 조회 (cache-aside)`() = runTest(timeout = 30.seconds) {
        nearCache.put(3L, "back-cache-val")
        nearCache.clear()  // front cache만 초기화 (NearSuspendCache.clear())

        // Front Cache miss → Back Cache(Ignite)에서 조회 (cache-aside 패턴)
        val result = nearCache.get(3L)
        result shouldBeEqualTo "back-cache-val"
    }

    @Test
    fun `putAll로 여러 항목 일괄 저장 후 getAll로 조회`() = runTest(timeout = 30.seconds) {
        val entries = mapOf(10L to "ten", 20L to "twenty", 30L to "thirty")
        nearCache.putAll(entries)

        val result = nearCache.getAll(setOf(10L, 20L, 30L)).toList()
            .associate { it.key to it.value }
        result.size shouldBeEqualTo 3
        result[10L] shouldBeEqualTo "ten"
        result[20L] shouldBeEqualTo "twenty"
        result[30L] shouldBeEqualTo "thirty"
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
    fun `containsKey는 Front Cache 초기화 후 Back Cache에서도 확인 (cache-aside)`() = runTest(timeout = 30.seconds) {
        nearCache.put(110L, "one-ten")
        nearCache.clear()

        // Front Cache miss → Back Cache(Ignite)에서 확인 (cache-aside)
        nearCache.containsKey(110L).shouldBeTrue()
    }
}
