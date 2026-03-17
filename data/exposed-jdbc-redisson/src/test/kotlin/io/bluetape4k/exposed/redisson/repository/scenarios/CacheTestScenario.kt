package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.exposed.redisson.repository.JdbcRedissonRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.junit.jupiter.api.BeforeEach

interface CacheTestScenario<ID: Any, E: Any> {
    companion object: KLogging() {
        @JvmStatic
        fun enableDialects() = setOf(TestDB.H2) // TestDB.enabledDialects()

        const val ENABLE_DIALECTS_METHOD = "enableDialects"
    }

    /**
     * 테스트에 사용할 캐시 설정
     */
    val cacheConfig: RedissonCacheConfig

    /**
     * 테스트에 사용할 캐시 저장소
     */
    val repository: JdbcRedissonRepository<ID, E>

    /**
     * 테스트에 사용할 테이블을 설정하고 테스트 로직을 실행하는 함수
     */
    fun withEntityTable(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    )

    /**
     * 테스트에서 사용할 샘플 ID를 반환합니다
     */
    fun getExistingId(): ID

    fun getExistingIds(): List<ID>

    /**
     * 테스트에서 사용할 존재하지 않는 ID를 반환합니다
     */
    fun getNonExistentId(): ID

    @BeforeEach
    fun setup() {
        // 테스트마다 기존 캐시를 비웁니다.
        repository.invalidateAll()
    }
}
