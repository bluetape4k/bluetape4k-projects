package io.bluetape4k.exposed.cache.scenarios

import java.io.Serializable

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.JdbcCacheRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.junit.jupiter.api.BeforeEach

/**
 * [JdbcCacheRepository] 기반 캐시 테스트의 공통 시나리오 인터페이스입니다.
 *
 * 각 테스트 메서드 실행 전에 캐시를 초기화하고,
 * 테스트에 필요한 샘플 데이터 접근 메서드를 정의합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
interface JdbcCacheTestScenario<ID: Any, E: Serializable> {

    companion object: KLogging()

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
    val repository: JdbcCacheRepository<ID, E>

    /**
     * 테스트에 사용할 테이블을 설정하고 테스트 로직을 실행하는 함수
     *
     * @param testDB 테스트 대상 DB
     * @param statement 테스트 로직
     */
    fun withEntityTable(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    )

    /**
     * 테스트에서 사용할 존재하는 샘플 ID를 반환합니다.
     *
     * @return 존재하는 엔티티의 식별자
     */
    fun getExistingId(): ID

    /**
     * DB에 존재하는 복수 샘플 ID를 반환합니다.
     *
     * @return 존재하는 엔티티의 식별자 목록
     */
    fun getExistingIds(): List<ID>

    /**
     * 테스트에서 사용할 존재하지 않는 ID를 반환합니다.
     *
     * @return DB와 캐시에 존재하지 않는 식별자
     */
    fun getNonExistentId(): ID

    /**
     * 테스트마다 기존 캐시를 비웁니다.
     */
    @BeforeEach
    fun beforeEach() {
        repository.clear()
    }
}
