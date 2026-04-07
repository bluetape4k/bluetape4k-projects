package io.bluetape4k.exposed.cache.scenarios

import java.io.Serializable

import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration

/**
 * [R2dbcCacheTestScenario] 기반 Write-Behind 캐시 suspend 시나리오입니다.
 *
 * R2DBC 환경에서 캐시에 먼저 저장하고, DB에는 비동기로 반영되는 패턴을 검증합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
@Suppress("DEPRECATION")
interface R2dbcWriteBehindScenario<ID: Any, E: Serializable>: R2dbcCacheTestScenario<ID, E> {

    companion object: KLoggingChannel() {
        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"
    }

    /**
     * 새로운 엔티티를 생성합니다.
     *
     * @return 새로 생성된 엔티티
     */
    suspend fun createNewEntity(): E

    /**
     * 지정한 개수만큼 새로운 엔티티 목록을 생성합니다.
     *
     * @param count 생성할 엔티티 수
     * @return 새로 생성된 엔티티 목록
     */
    suspend fun createNewEntities(count: Int): List<E> = List(count) { createNewEntity() }

    /**
     * DB에서 전체 레코드 수를 조회합니다.
     *
     * @return 전체 레코드 수
     */
    suspend fun getAllCountFromDB(): Long =
        suspendTransaction {
            repository.table.selectAll().count()
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Write-Behind로 대량의 데이터를 추가합니다`(testDB: TestDB) = runTest {
        // AutoInc 테이블은 DB가 ID를 할당하므로 Write-Behind로 신규 엔티티를 삽입하지 않는다
        Assumptions.assumeTrue(repository.table.id.autoIncColumnType == null) {
            "AutoInc 테이블은 Write-Behind로 신규 엔티티를 DB에 삽입하지 않아 이 테스트를 건너뜁니다"
        }
        withR2dbcEntityTable(testDB) {
            val entities = createNewEntities(1000)
            val entityMap = entities.associateBy { repository.extractId(it) }
            repository.putAll(entityMap)

            await
                .atMost(Duration.ofSeconds(30))
                .withPollInterval(Duration.ofSeconds(5))
                .until { kotlinx.coroutines.runBlocking { getAllCountFromDB() } > entities.size.toLong() }

            // DB에서 조회한 값
            val dbCount = getAllCountFromDB()
            dbCount shouldBeGreaterThan entities.size.toLong()
        }
    }
}
