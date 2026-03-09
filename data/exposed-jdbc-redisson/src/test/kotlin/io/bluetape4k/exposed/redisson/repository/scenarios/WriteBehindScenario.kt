package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration

interface WriteBehindScenario<ID: Any,T: IdTable<ID>,  E: HasIdentifier<ID>>: CacheTestScenario<ID, T, E> {

    companion object: KLogging()

    fun createNewEntity(): E

    fun createNewEntities(count: Int): List<E> {
        return List(count) { createNewEntity() }
    }

    fun getAllCountFromDB() = transaction {
        repository.entityTable.selectAll().count()
    }

    @ParameterizedTest
    @MethodSource(CacheTestScenario.ENABLE_DIALECTS_METHOD)
    fun `Write Behind 로 대량의 데이터를 추가합니다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val entities = createNewEntities(1000)
            repository.putAll(entities)

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
