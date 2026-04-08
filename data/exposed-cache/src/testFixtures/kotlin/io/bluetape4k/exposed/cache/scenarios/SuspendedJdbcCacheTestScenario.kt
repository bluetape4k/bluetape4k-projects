package io.bluetape4k.exposed.cache.scenarios

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.SuspendedJdbcCacheRepository
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcCacheTestScenario.Companion.DefaultCacheDispatcher
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.junit.jupiter.api.BeforeEach
import java.io.Serializable
import kotlin.coroutines.CoroutineContext

/**
 * [SuspendedJdbcCacheRepository] 기반 캐시 테스트의 공통 suspend 시나리오 인터페이스입니다.
 *
 * 각 테스트 메서드 실행 전에 캐시를 초기화하고,
 * 테스트에 필요한 샘플 데이터 접근 메서드를 정의합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
interface SuspendedJdbcCacheTestScenario<ID: Any, E: Serializable> {

    companion object: KLoggingChannel() {
        val DefaultCacheDispatcher: CoroutineContext = Dispatchers.IO
    }

    /**
     * 캐시 쓰기 전략
     */
    val cacheWriteMode: CacheWriteMode

    /**
     * 캐시 저장 방식
     */
    val cacheMode: CacheMode

    /**
     * 테스트에 사용할 캐시 저장소
     */
    val repository: SuspendedJdbcCacheRepository<ID, E>

    /**
     * 테스트에 사용할 테이블을 설정하고 suspend 테스트 로직을 실행하는 함수
     *
     * @param testDB 테스트 대상 DB
     * @param context 코루틴 컨텍스트 (기본값: [DefaultCacheDispatcher])
     * @param statement suspend 테스트 로직
     */
    suspend fun withSuspendedEntityTable(
        testDB: TestDB,
        context: CoroutineContext = DefaultCacheDispatcher,
        statement: suspend JdbcTransaction.() -> Unit,
    )

    /**
     * 테스트에서 사용할 존재하는 샘플 ID를 반환합니다.
     *
     * @return 존재하는 엔티티의 식별자
     */
    suspend fun getExistingId(): ID

    /**
     * DB에 존재하는 복수 샘플 ID를 반환합니다.
     *
     * @return 존재하는 엔티티의 식별자 목록
     */
    suspend fun getExistingIds(): List<ID>

    /**
     * 테스트에서 사용할 존재하지 않는 ID를 반환합니다.
     *
     * @return DB와 캐시에 존재하지 않는 식별자
     */
    suspend fun getNonExistentId(): ID

    /**
     * 테스트마다 기존 캐시를 비웁니다.
     */
    @BeforeEach
    fun beforeEach() {
        runBlocking(DefaultCacheDispatcher) {
            repository.clear()
        }
    }
}
