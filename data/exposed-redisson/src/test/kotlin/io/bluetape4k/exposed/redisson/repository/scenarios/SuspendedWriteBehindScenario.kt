package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.exposed.core.HasIdentifier
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
interface SuspendedWriteBehindScenario<T: HasIdentifier<ID>, ID: Any>: SuspendedCacheTestScenario<T, ID> {

    companion object: KLoggingChannel()

    suspend fun createNewEntity(): T

    suspend fun createNewEntities(count: Int): List<T> {
        return fastList(count) { createNewEntity() }
    }

    suspend fun getAllCountFromDB() = newSuspendedTransaction {
        repository.entityTable.selectAll().count()
    }

    @ParameterizedTest
    @MethodSource(CacheTestScenario.ENABLE_DIALECTS_METHOD)
    fun `Write Behind 로 대량의 데이터를 추가합니다`(testDB: TestDB) = runSuspendIO {
        withSuspendedEntityTable(testDB) {
            val entities = createNewEntities(1000)
            repository.putAll(entities)

            await
                .atMost(Duration.ofSeconds(10))
                .withPollInterval(Duration.ofMillis(1000))
                .untilSuspending { getAllCountFromDB() > entities.size.toLong() }

            // DB에서 조회한 값
            val dbCount = getAllCountFromDB()
            dbCount shouldBeGreaterThan entities.size.toLong()
        }
    }

}
