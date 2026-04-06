package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.exposed.redisson.AbstractRedissonTest.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration

@Suppress("DEPRECATION")
interface SuspendedWriteBehindScenario<ID: Any, E: Any>: SuspendedCacheTestScenario<ID, E> {
    companion object: KLoggingChannel()

    suspend fun createNewEntity(): E

    suspend fun createNewEntities(count: Int): List<E> = List(count) { createNewEntity() }

    suspend fun getAllCountFromDB() =
        newSuspendedTransaction {
            repository.table.selectAll().count()
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Write Behind 로 대량의 데이터를 추가합니다`(testDB: TestDB) =
        runSuspendIO {
            withSuspendedEntityTable(testDB) {
                val entities = createNewEntities(1000)
                repository.putAll(entities)

                await
                    .atMost(Duration.ofSeconds(30))
                    .withPollInterval(Duration.ofSeconds(5))
                    .untilSuspending { getAllCountFromDB() > entities.size.toLong() }

                // DB에서 조회한 값
                val dbCount = getAllCountFromDB()
                dbCount shouldBeGreaterThan entities.size.toLong()
            }
        }
}
