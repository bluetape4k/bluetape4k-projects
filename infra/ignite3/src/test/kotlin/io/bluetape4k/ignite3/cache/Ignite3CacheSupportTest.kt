package io.bluetape4k.ignite3.cache

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.ignite3.memorizer.memorizer
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.apache.ignite.table.KeyValueView
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [keyValueView], [nearCache] 확장 함수 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 3.x 서버를 실행하고 [keyValueView], [nearCache] 확장 함수를 테스트합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ignite3CacheSupportTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val TABLE_NAME = "KV_SUPPORT_TABLE"
    }

    @BeforeAll
    fun createTable() {
        igniteClient.sql().execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ID BIGINT PRIMARY KEY,
                DATA VARCHAR(255)
            )
            """.trimIndent()
        ).close()
    }

    @BeforeEach
    fun setup() {
        igniteClient.sql().execute(null, "DELETE FROM $TABLE_NAME").close()
    }

    @Test
    fun `keyValueView 확장 함수로 KeyValueView 생성 및 put-get 동작 확인`() {
        val view: KeyValueView<Long, String> = igniteClient.keyValueView(TABLE_NAME)

        view.shouldNotBeNull()

        view.put(null, 1L, "hello")
        view.put(null, 2L, "world")

        view.get(null, 1L) shouldBeEqualTo "hello"
        view.get(null, 2L) shouldBeEqualTo "world"
        view.get(null, 9999L).shouldBeNull()
    }

    @Test
    fun `nearCache 확장 함수로 IgniteNearCache 생성 및 CRUD 동작 확인`() {
        val config = igniteNearCacheConfig<Long, String>(tableName = TABLE_NAME)
        val cache: IgniteNearCache<Long, String> = igniteClient.nearCache(config)

        cache.shouldNotBeNull()

        // put / get
        cache.put(10L, "ten")
        cache.get(10L) shouldBeEqualTo "ten"

        // 존재하지 않는 키 조회
        cache.get(9999L).shouldBeNull()

        // containsKey
        cache.containsKey(10L).shouldBeTrue()
        cache.containsKey(9999L).shouldBeFalse()

        // remove
        val removed = cache.remove(10L)
        removed.shouldBeTrue()
        cache.get(10L).shouldBeNull()

        // 존재하지 않는 키 remove
        val notRemoved = cache.remove(9999L)
        notRemoved.shouldBeFalse()
    }

    @Test
    fun `keyValueView에서 memorizer 생성 및 캐싱 동작 확인`() {
        val view: KeyValueView<Long, String> = igniteClient.keyValueView(TABLE_NAME)
        var callCount = 0

        val memo = view.memorizer { key ->
            callCount++
            "value-$key"
        }

        // 첫 번째 호출 - evaluator 실행
        val result1 = memo(100L)
        result1 shouldBeEqualTo "value-100"
        callCount shouldBeEqualTo 1

        // 동일한 키 재호출 - evaluator를 다시 실행하지 않음
        val result2 = memo(100L)
        result2 shouldBeEqualTo "value-100"
        callCount shouldBeEqualTo 1

        // 다른 키 호출 - evaluator 실행
        val result3 = memo(200L)
        result3 shouldBeEqualTo "value-200"
        callCount shouldBeEqualTo 2
    }
}
