package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.exposed.cache.SuspendedJdbcCacheRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.junit.jupiter.api.BeforeEach
import kotlin.coroutines.CoroutineContext

interface SuspendedCacheTestScenario<ID: Any, E: java.io.Serializable> {
    companion object: KLoggingChannel() {
        val DefaultCacheDispatcher = Dispatchers.IO
    }

    /**
     * 테스트에 사용할 캐시 설정
     */
    val cacheConfig: RedissonCacheConfig

    /**
     * 테스트에 사용할 캐시 저장소
     */
    val repository: SuspendedJdbcCacheRepository<ID, E>

    /**
     * 테스트에 사용할 테이블을 설정하고 테스트 로직을 실행하는 함수
     */
    suspend fun withSuspendedEntityTable(
        testDB: TestDB,
        context: CoroutineContext = DefaultCacheDispatcher,
        statement: suspend JdbcTransaction.() -> Unit,
    )

    /**
     * 테스트에서 사용할 샘플 ID를 반환합니다
     */
    suspend fun getExistingId(): ID

    suspend fun getExistingIds(): List<ID>

    /**
     * 테스트에서 사용할 존재하지 않는 ID를 반환합니다
     */
    suspend fun getNonExistentId(): ID

    @BeforeEach
    fun beforeEach() {
        // 테스트마다 기존 캐시를 비웁니다.
        runBlocking(DefaultCacheDispatcher) {
            repository.clear()
        }
    }
}
