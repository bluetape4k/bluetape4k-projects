package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.scenarios.JdbcCacheTestScenario
import io.bluetape4k.exposed.lettuce.repository.JdbcLettuceRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.WriteMode

/**
 * exposed-jdbc-lettuce 통합 테스트 시나리오 베이스 인터페이스.
 *
 * - [JdbcCacheTestScenario]를 확장하여 testFixtures 시나리오 재사용
 * - `@BeforeEach`에서 캐시를 비운다.
 * - 서브 인터페이스(ReadThroughScenario 등)가 테스트 메서드를 추가한다.
 * - 구현 클래스는 withEntityTable(testDB, statement)를 오버라이드하여 테이블 설정을 담당한다.
 */
interface CacheTestScenario<ID: Any, E: java.io.Serializable>: JdbcCacheTestScenario<ID, E> {
    companion object : KLogging()

    /** 테스트 대상 레포지토리 (JdbcLettuceRepository는 JdbcCacheRepository를 구현하므로 공변 오버라이드) */
    override val repository: JdbcLettuceRepository<ID, E>

    /** 적용된 캐시 설정 */
    val config: LettuceCacheConfig

    /** 캐시 쓰기 전략 — config.writeMode에서 파생 */
    override val cacheWriteMode: CacheWriteMode
        get() = when (config.writeMode) {
            WriteMode.NONE -> CacheWriteMode.READ_ONLY
            WriteMode.WRITE_THROUGH -> CacheWriteMode.WRITE_THROUGH
            WriteMode.WRITE_BEHIND -> CacheWriteMode.WRITE_BEHIND
        }

    /** 캐시 저장 방식 — config.nearCacheEnabled에서 파생 */
    override val cacheMode: CacheMode
        get() = if (config.nearCacheEnabled) CacheMode.NEAR_CACHE else CacheMode.REMOTE
}
