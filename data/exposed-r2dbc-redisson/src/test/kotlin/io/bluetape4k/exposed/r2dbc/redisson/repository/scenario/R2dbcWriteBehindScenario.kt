package io.bluetape4k.exposed.r2dbc.redisson.repository.scenario

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.repository.scenario.R2dbcCacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration

interface R2dbcWriteBehindScenario<T: HasIdentifier<ID>, ID: Any>: R2dbcCacheTestScenario<T, ID> {

    companion object: KLoggingChannel()

    suspend fun createNewEntity(): T

    suspend fun createNewEntities(count: Int): List<T> {
        return fastList(count) { createNewEntity() }
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
}
