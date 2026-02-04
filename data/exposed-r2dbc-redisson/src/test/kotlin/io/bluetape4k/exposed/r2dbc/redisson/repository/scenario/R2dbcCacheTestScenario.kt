package io.bluetape4k.exposed.r2dbc.redisson.repository.scenario

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.repository.R2dbcCacheRepository
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.junit.jupiter.api.BeforeEach
import kotlin.coroutines.CoroutineContext

interface R2dbcCacheTestScenario<T: HasIdentifier<ID>, ID: Any> {

    companion object: KLoggingChannel() {
        @JvmStatic
        fun enableDialects() =
            setOf(TestDB.H2) // setOf(TestDB.MYSQL_V8) // setOf(TestDB.POSTGRESQL) //  TestDB.enabledDialects()

        const val ENABLE_DIALECTS_METHOD = "enableDialects"

        val DefaultCacheDispatcher = Dispatchers.IO
    }

    /**
     * 테스트에 사용할 캐시 설정
     */
    val cacheConfig: RedisCacheConfig

    /**
     * 테스트에 사용할 캐시 저장소
     */
    val repository: R2dbcCacheRepository<T, ID>

    /**
     * 테스트에 사용할 테이블을 설정하고 테스트 로직을 실행하는 함수
     */
    suspend fun withR2dbcEntityTable(
        testDB: TestDB,
        context: CoroutineContext = DefaultCacheDispatcher,
        statement: suspend R2dbcTransaction.() -> Unit,
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
    fun setup() {
        // 테스트마다 기존 캐시를 비웁니다.
        runBlocking(DefaultCacheDispatcher) {
            repository.invalidateAll()
        }
    }
}
