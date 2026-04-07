package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcCacheTestScenario
import io.bluetape4k.exposed.lettuce.repository.SuspendedJdbcLettuceRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.WriteMode

/**
 * exposed-jdbc-lettuce suspend 통합 테스트 시나리오 베이스 인터페이스.
 *
 * - [SuspendedJdbcCacheTestScenario]를 확장하여 testFixtures 시나리오 재사용
 * - `@BeforeEach`에서 캐시를 비운다.
 * - 서브 인터페이스(SuspendedReadThroughScenario 등)가 테스트 메서드를 추가한다.
 */
@Suppress("DEPRECATION")
interface SuspendedCacheTestScenario<ID: Any, E: java.io.Serializable>: SuspendedJdbcCacheTestScenario<ID, E> {
    companion object : KLoggingChannel()

    /** 테스트 대상 suspend 레포지토리 (SuspendedJdbcLettuceRepository는 SuspendedJdbcCacheRepository를 구현하므로 공변 오버라이드) */
    override val repository: SuspendedJdbcLettuceRepository<ID, E>

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
