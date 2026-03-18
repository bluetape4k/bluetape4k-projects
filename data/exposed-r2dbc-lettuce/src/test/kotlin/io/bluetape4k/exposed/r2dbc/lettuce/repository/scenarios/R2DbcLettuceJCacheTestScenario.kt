package io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios

import io.bluetape4k.exposed.r2dbc.lettuce.repository.R2dbcLettuceRepository
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.junit.jupiter.api.BeforeEach

/**
 * R2DBC Lettuce 캐시 테스트 시나리오 베이스 인터페이스.
 *
 * - `@BeforeEach`에서 캐시를 비운다.
 * - 서브 인터페이스(Read-through / Write-through / Write-behind)가 테스트 메서드를 추가한다.
 */
interface R2DbcLettuceJCacheTestScenario<ID: Any, E: Any> {
    companion object: KLoggingChannel() {
        @JvmStatic
        fun enableDialects() = setOf(TestDB.H2)

        const val ENABLE_DIALECTS_METHOD = "enableDialects"
    }

    /** 테스트 대상 레포지토리 */
    val repository: R2dbcLettuceRepository<ID, E>

    /** 적용된 캐시 설정 */
    val config: LettuceCacheConfig

    /**
     * 테스트용 테이블을 생성하고 초기 데이터를 삽입한 뒤 [statement]를 실행한다.
     */
    suspend fun withR2dbcEntityTable(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.() -> Unit,
    )

    /** DB에 존재하는 샘플 ID를 반환한다 */
    suspend fun getExistingId(): ID

    /** DB에 존재하는 복수 샘플 ID를 반환한다 */
    suspend fun getExistingIds(): List<ID>

    /** DB와 캐시 모두에 존재하지 않는 ID를 반환한다 */
    fun getNonExistentId(): ID

    @BeforeEach
    fun clearCacheBeforeEach() {
        runTest { repository.clearCache() }
    }
}
