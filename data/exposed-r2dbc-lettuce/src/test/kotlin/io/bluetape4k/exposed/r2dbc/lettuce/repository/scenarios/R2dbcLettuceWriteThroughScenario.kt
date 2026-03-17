package io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios

import io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios.R2dbcLettuceCacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Write-through 캐시 전략 R2DBC Lettuce 시나리오.
 *
 * - save() 시 Redis와 DB를 즉시 갱신
 * - delete() 시 Redis와 DB를 모두 삭제
 */
interface R2dbcLettuceWriteThroughScenario<ID: Any, E: Any>: R2dbcLettuceCacheTestScenario<ID, E> {
    companion object: KLoggingChannel()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    suspend fun updateEmail(entity: E): E

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `save - 캐시와 DB 모두에 즉시 반영된다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                val entity = repository.findByIdFromDb(id).shouldNotBeNull()
                val updated = updateEmail(entity)
                repository.save(id, updated)

                repository.findById(id) shouldBeEqualTo updated
                repository.findByIdFromDb(id) shouldBeEqualTo updated
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `saveAll - Map 일괄 저장 후 캐시와 DB 모두 반영된다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val ids = getExistingIds()
                val entities = repository.findAll(ids)
                val updated = entities.mapValues { (_, v) -> updateEmail(v) }
                repository.saveAll(updated)

                updated.forEach { (id, entity) ->
                    repository.findById(id) shouldBeEqualTo entity
                    repository.findByIdFromDb(id) shouldBeEqualTo entity
                }
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete - 캐시와 DB 모두에서 삭제된다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                repository.save(id, repository.findByIdFromDb(id)!!)
                repository.delete(id)

                repository.findById(id).shouldBeNull()
                repository.findByIdFromDb(id).shouldBeNull()
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `deleteAll - 복수 ID를 한번에 삭제한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val ids = getExistingIds()
                ids.forEach { id -> repository.save(id, repository.findByIdFromDb(id)!!) }
                repository.deleteAll(ids)

                ids.forEach { id ->
                    repository.findById(id).shouldBeNull()
                    repository.findByIdFromDb(id).shouldBeNull()
                }
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findByIdFromDb - DB 직접 조회가 정상 동작한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                repository.findByIdFromDb(id).shouldNotBeNull()
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAllFromDb - 복수 ID DB 직접 조회가 정상 동작한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val ids = getExistingIds()
                val entities = repository.findAllFromDb(ids)
                entities.size shouldBeEqualTo ids.size
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countFromDb - DB 전체 레코드 수를 반환한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                repository.countFromDb() shouldBeEqualTo getExistingIds().size.toLong()
            }
        }
}
