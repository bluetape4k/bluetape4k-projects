package io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios

import io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios.R2DbcLettuceJCacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Write-behind 캐시 전략 R2DBC Lettuce 시나리오.
 *
 * - put() 시 Redis에는 즉시 반영, DB는 비동기로 일괄 적재
 */
interface R2dbcLettuceWriteBehindScenario<ID: Any, E: Serializable>: R2DbcLettuceJCacheTestScenario<ID, E> {
    companion object: KLoggingChannel()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    suspend fun updateEmail(entity: E): E

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - WRITE_BEHIND 저장 후 Redis에는 즉시 반영된다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                val entity = repository.findByIdFromDb(id).shouldNotBeNull()
                val updated = updateEmail(entity)
                repository.put(id, updated)

                repository.get(id) shouldBeEqualTo updated
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - WRITE_BEHIND flush 주기 후 DB에도 반영된다`(testDB: TestDB) =
        runTest(timeout = 10.seconds) {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                val entity = repository.findByIdFromDb(id).shouldNotBeNull()
                val updated = updateEmail(entity)
                repository.put(id, updated)

                await
                    .atMost(Duration.ofSeconds(10))
                    .withPollInterval(Duration.ofMillis(100))
                    .until { runBlocking { repository.findByIdFromDb(id) == updated } }

                repository.findByIdFromDb(id) shouldBeEqualTo updated
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - 여러 레코드를 배치로 비동기 적재한다`(testDB: TestDB) =
        runTest(timeout = 10.seconds) {
            withR2dbcEntityTable(testDB) {
                val ids = getExistingIds()
                val entities = ids.associateWith { id -> updateEmail(repository.findByIdFromDb(id)!!) }
                repository.putAll(entities)

                await
                    .atMost(Duration.ofSeconds(10))
                    .withPollInterval(Duration.ofMillis(100))
                    .until { runBlocking { entities.all { (id, e) -> repository.findByIdFromDb(id) == e } } }

                entities.forEach { (id, expected) ->
                    repository.findByIdFromDb(id) shouldBeEqualTo expected
                }
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get - 존재하지 않는 ID는 null을 반환한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                repository.get(getNonExistentId()).shouldBeNull()
            }
        }
}
