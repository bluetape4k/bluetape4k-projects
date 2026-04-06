package io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios

import io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios.R2DbcLettuceJCacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Write-behind 캐시 전략 R2DBC Lettuce 시나리오.
 *
 * - save() 시 Redis에는 즉시 반영, DB는 비동기로 일괄 적재
 */
interface R2dbcLettuceWriteBehindScenario<ID: Any, E: Any>: R2DbcLettuceJCacheTestScenario<ID, E> {
    companion object: KLoggingChannel()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    suspend fun updateEmail(entity: E): E

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `save - WRITE_BEHIND 저장 후 Redis에는 즉시 반영된다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                val entity = repository.findByIdFromDb(id).shouldNotBeNull()
                val updated = updateEmail(entity)
                repository.save(id, updated)

                repository.findById(id) shouldBeEqualTo updated
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `save - WRITE_BEHIND flush 주기 후 DB에도 반영된다`(testDB: TestDB) =
        runTest(timeout = 10.seconds) {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                val entity = repository.findByIdFromDb(id).shouldNotBeNull()
                val updated = updateEmail(entity)
                repository.save(id, updated)

                // TODO: await 를 사용해야 한다
                // Write-behind는 비동기이므로 DB 반영까지 폴링
                val deadline = System.currentTimeMillis() + 10_000L
                while (repository.findByIdFromDb(id) != updated && System.currentTimeMillis() < deadline) {
                    delay(100.milliseconds)
                }

                repository.findByIdFromDb(id) shouldBeEqualTo updated
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `saveAll - 여러 레코드를 배치로 비동기 적재한다`(testDB: TestDB) =
        runTest(timeout = 10.seconds) {
            withR2dbcEntityTable(testDB) {
                val ids = getExistingIds()
                val entities = ids.associateWith { id -> updateEmail(repository.findByIdFromDb(id)!!) }
                repository.saveAll(entities)

                // TODO: await 를 사용해야 한다
                val deadline = System.currentTimeMillis() + 10_000L
                while (
                    entities.any { (id, e) -> repository.findByIdFromDb(id) != e } &&
                    System.currentTimeMillis() < deadline
                ) {
                    delay(100.milliseconds)
                }

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
                repository.findById(getNonExistentId()).shouldBeNull()
            }
        }
}
