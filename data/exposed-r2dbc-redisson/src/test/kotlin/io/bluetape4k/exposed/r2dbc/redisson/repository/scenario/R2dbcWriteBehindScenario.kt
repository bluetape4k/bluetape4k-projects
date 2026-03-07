package io.bluetape4k.exposed.r2dbc.redisson.repository.scenario

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.repository.scenario.R2dbcCacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration

interface R2dbcWriteBehindScenario<ID: Any, T: IdTable<ID>, E: HasIdentifier<ID>>: R2dbcCacheTestScenario<ID, T, E> {

    companion object: KLoggingChannel()

    suspend fun createNewEntity(): E

    suspend fun createNewEntities(count: Int): List<E> {
        return List(count) { createNewEntity() }
    }

    suspend fun getAllCountFromDB(): Long = suspendTransaction {
        repository.entityTable.selectAll().count()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Write Behind 로 대량의 데이터를 추가합니다`(testDB: TestDB) = runTest {
        withR2dbcEntityTable(testDB) {
            val entities = createNewEntities(1000)
            repository.putAll(entities)

            await
                .atMost(Duration.ofSeconds(10))
                .withPollInterval(Duration.ofSeconds(1))
                .untilSuspending { getAllCountFromDB() >= entities.size.toLong() }

            // DB에서 조회한 값
            val dbCount = getAllCountFromDB()
            dbCount shouldBeGreaterThan entities.size.toLong()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put 단건 캐시 저장 후 Write-Behind로 DB에 비동기 반영된다`(testDB: TestDB) = runTest {
        withR2dbcEntityTable(testDB) {
            val entity = createNewEntity()
            repository.put(entity)

            // Write-Behind는 비동기이므로 잠시 대기 후 DB에서 확인
            await
                .atMost(Duration.ofSeconds(10))
                .withPollInterval(Duration.ofMillis(500))
                .untilSuspending { getAllCountFromDB() >= 1L }

            val dbCount = getAllCountFromDB()
            dbCount shouldBeGreaterThan 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get - 존재하지 않는 ID는 null을 반환한다`(testDB: TestDB) = runTest {
        withR2dbcEntityTable(testDB) {
            val nonExistentId = getNonExistentId()
            val result = repository.get(nonExistentId)
            result.shouldBeNull()
        }
    }
}
