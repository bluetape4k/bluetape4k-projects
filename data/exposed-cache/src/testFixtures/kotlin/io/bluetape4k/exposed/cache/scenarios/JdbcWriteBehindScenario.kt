package io.bluetape4k.exposed.cache.scenarios

import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.time.Duration

/**
 * [JdbcCacheTestScenario] 기반 Write-Behind 캐시 시나리오입니다.
 *
 * 캐시에 먼저 저장하고, DB에는 비동기로 반영되는 패턴을 검증합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
interface JdbcWriteBehindScenario<ID: Any, E: Serializable>: JdbcCacheTestScenario<ID, E> {

    companion object: KLogging() {
        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"
    }

    /**
     * 새로운 엔티티를 생성합니다.
     *
     * @return 새로 생성된 엔티티
     */
    fun createNewEntity(): E

    /**
     * 지정한 개수만큼 새로운 엔티티 목록을 생성합니다.
     *
     * @param count 생성할 엔티티 수
     * @return 새로 생성된 엔티티 목록
     */
    fun createNewEntities(count: Int): List<E> = List(count) { createNewEntity() }

    /**
     * DB에서 전체 레코드 수를 조회합니다.
     *
     * @return 전체 레코드 수
     */
    fun getAllCountFromDB(): Long =
        transaction {
            repository.table.selectAll().count()
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Write-Behind로 대량의 데이터를 추가합니다`(testDB: TestDB) {
        // AutoInc 테이블은 DB가 ID를 할당하므로 Write-Behind로 신규 엔티티를 삽입하지 않는다
        Assumptions.assumeTrue(repository.table.id.autoIncColumnType == null) {
            "AutoInc 테이블은 Write-Behind로 신규 엔티티를 DB에 삽입하지 않아 이 테스트를 건너뜁니다"
        }
        withEntityTable(testDB) {
            val entities = createNewEntities(1000)
            val entityMap = entities.associateBy { repository.extractId(it) }
            repository.putAll(entityMap)

            await
                .atMost(Duration.ofSeconds(30))
                .withPollInterval(Duration.ofMillis(5))
                .until { getAllCountFromDB() >= entities.size.toLong() }

            // DB에서 조회한 값
            val dbCount = getAllCountFromDB()
            dbCount shouldBeGreaterThan entities.size.toLong()
        }
    }
}
