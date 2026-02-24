package io.bluetape4k.ignite3.cache

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [IgniteNearCache] ([NearCache])의 CRUD 동작을 검증하는 통합 테스트입니다.
 *
 * NearCache는 put 시 front와 back 모두에 저장합니다.
 * get 시에는 front cache만 조회합니다 (Redis NearCache와 동일한 패턴).
 * Ignite 3.x 씬 클라이언트는 이벤트 리스너를 지원하지 않으므로
 * back cache 변경이 front cache에 즉시 전파되지 않습니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteNearCacheTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val TABLE_NAME = "TEST_CACHE"
    }

    private lateinit var nearCache: IgniteNearCache<Long, String>

    @BeforeEach
    fun setup() {
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        nearCache = igniteClient.nearCacheManager().nearCache(config)
        nearCache.clear()  // front cache만 초기화
        igniteClient.sql().execute(null, "DELETE FROM $TABLE_NAME").close()
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() {
        val result = nearCache.get(9999L)
        result.shouldBeNull()
    }

    @Test
    fun `put으로 저장 후 get으로 조회 가능`() {
        nearCache.put(1L, "hello")
        val result = nearCache.get(1L)
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `Front Cache에 캐시된 값이 반환됨`() {
        nearCache.put(2L, "cached")
        val result = nearCache.get(2L)
        result shouldBeEqualTo "cached"
    }

    @Test
    fun `putAll로 여러 항목 일괄 저장 후 getAll로 조회`() {
        val entries = mapOf(10L to "ten", 20L to "twenty", 30L to "thirty")
        nearCache.putAll(entries)

        val result = nearCache.getAll(setOf(10L, 20L, 30L))
        result.size shouldBeEqualTo 3
        result[10L] shouldBeEqualTo "ten"
        result[20L] shouldBeEqualTo "twenty"
        result[30L] shouldBeEqualTo "thirty"
    }

    @Test
    fun `remove로 Front Cache와 Back Cache에서 항목 삭제`() {
        nearCache.put(60L, "sixty")
        val removed = nearCache.remove(60L)
        removed.shouldBeTrue()
        nearCache.get(60L).shouldBeNull()
    }

    @Test
    fun `존재하지 않는 키를 remove하면 false 반환`() {
        val removed = nearCache.remove(9998L)
        removed.shouldBeFalse()
    }

    @Test
    fun `containsKey로 키 존재 여부 확인`() {
        nearCache.put(100L, "hundred")
        nearCache.containsKey(100L).shouldBeTrue()
        nearCache.containsKey(9997L).shouldBeFalse()
    }
}
