package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.lettuce.repository.JdbcLettuceRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.junit.jupiter.api.BeforeEach

/**
 * exposed-jdbc-lettuce 통합 테스트 시나리오 베이스 인터페이스.
 *
 * - `@BeforeEach setup()`에서 캐시를 비운다.
 * - 서브 인터페이스(ReadThroughScenario 등)가 테스트 메서드를 추가한다.
 * - 구현 클래스는 @BeforeAll setupDb(), @BeforeEach setupData(), @AfterEach tearDown()을 담당한다.
 */
interface CacheTestScenario<ID : Comparable<ID>, E : Any> {
    companion object : KLogging()

    /** 테스트 대상 레포지토리 */
    val repository: JdbcLettuceRepository<ID, E>

    /** 적용된 캐시 설정 */
    val config: LettuceCacheConfig

    /** DB에 존재하는 샘플 ID를 반환한다 */
    fun getExistingId(): ID

    /** DB에 존재하는 복수 샘플 ID를 반환한다 */
    fun getExistingIds(): List<ID>

    /** DB와 캐시 모두에 존재하지 않는 ID를 반환한다 */
    fun getNonExistentId(): ID

    @BeforeEach
    fun clearCacheBeforeEach() {
        repository.clearCache()
    }
}
