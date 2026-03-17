package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.lettuce.repository.SuspendedJdbcLettuceRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach

/**
 * exposed-jdbc-lettuce suspend 통합 테스트 시나리오 베이스 인터페이스.
 *
 * - `@BeforeEach`에서 캐시를 비운다.
 * - 서브 인터페이스(SuspendedReadThroughScenario 등)가 테스트 메서드를 추가한다.
 */
interface SuspendedCacheTestScenario<ID : Comparable<ID>, E : Any> {
    companion object : KLoggingChannel()

    /** 테스트 대상 suspend 레포지토리 */
    val repository: SuspendedJdbcLettuceRepository<ID, E>

    /** 적용된 캐시 설정 */
    val config: LettuceCacheConfig

    /** DB에 존재하는 샘플 ID를 반환한다 */
    suspend fun getExistingId(): ID

    /** DB에 존재하는 복수 샘플 ID를 반환한다 */
    suspend fun getExistingIds(): List<ID>

    /** DB와 캐시 모두에 존재하지 않는 ID를 반환한다 */
    suspend fun getNonExistentId(): ID

    @BeforeEach
    fun clearCacheBeforeEach() {
        runTest { repository.clearCache() }
    }
}
