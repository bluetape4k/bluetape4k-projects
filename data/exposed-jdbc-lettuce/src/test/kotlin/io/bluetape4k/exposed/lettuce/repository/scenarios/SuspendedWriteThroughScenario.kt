package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcWriteThroughScenario
import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Write-through 캐시 전략 suspend 시나리오.
 *
 * - [SuspendedJdbcWriteThroughScenario]를 확장하여 testFixtures의 @ParameterizedTest 시나리오도 포함
 * - put() 시 캐시와 DB를 동시에 갱신
 * - invalidate() 시 캐시와 DB를 모두 삭제
 */
interface SuspendedWriteThroughScenario<ID : Any, E : java.io.Serializable> :
    SuspendedJdbcWriteThroughScenario<ID, E>,
    SuspendedCacheTestScenario<ID, E> {

    companion object : KLoggingChannel()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    suspend fun updateEmail(entity: E): E

    // SuspendedJdbcWriteThroughScenario.updateEntityEmail 을 updateEmail 로 위임
    override suspend fun updateEntityEmail(entity: E): E = updateEmail(entity)

    // SuspendedJdbcWriteThroughScenario.assertSameEntityWithoutAudit — 기본 equality 검사
    override suspend fun assertSameEntityWithoutAudit(entity1: E, entity2: E) {
        entity1 shouldBeEqualTo entity2
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - 캐시와 DB 모두에 반영된다`(testDB: TestDB) =
        runTest {
            withSuspendedEntityTable(testDB) {
                val id = getExistingId()
                val entity = repository.findByIdFromDb(id).shouldNotBeNull()
                val updated = updateEmail(entity)
                repository.put(id, updated)

                repository.get(id) shouldBeEqualTo updated
                repository.findByIdFromDb(id) shouldBeEqualTo updated
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - Map 일괄 저장 후 캐시와 DB 모두 반영된다`(testDB: TestDB) =
        runTest {
            withSuspendedEntityTable(testDB) {
                val ids = getExistingIds()
                val entities = repository.getAll(ids)
                val updated = entities.mapValues { (_, v) -> updateEmail(v) }
                repository.putAll(updated)

                updated.forEach { (id, entity) ->
                    repository.get(id) shouldBeEqualTo entity
                    repository.findByIdFromDb(id) shouldBeEqualTo entity
                }
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidate - 캐시에서만 삭제되고 DB는 유지된다`(testDB: TestDB) =
        runTest {
            withSuspendedEntityTable(testDB) {
                val id = getExistingId()
                val entity = repository.findByIdFromDb(id).shouldNotBeNull()
                repository.put(id, entity)
                repository.invalidate(id)

                // DB에는 여전히 존재한다 (invalidate는 캐시만 제거)
                repository.findByIdFromDb(id) shouldBeEqualTo entity
                // get()은 캐시 미스 후 DB Read-Through로 다시 로드
                repository.get(id) shouldBeEqualTo entity
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidateAll - 복수 ID를 캐시에서만 삭제하고 DB는 유지된다`(testDB: TestDB) =
        runTest {
            withSuspendedEntityTable(testDB) {
                val ids = getExistingIds()
                val entities = ids.associateWith { repository.findByIdFromDb(it).shouldNotBeNull() }
                ids.forEach { id -> repository.put(id, entities[id]!!) }
                repository.invalidateAll(ids)

                // DB에는 모두 여전히 존재한다
                ids.forEach { id ->
                    repository.findByIdFromDb(id) shouldBeEqualTo entities[id]
                    repository.get(id) shouldBeEqualTo entities[id]
                }
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countFromDb - DB 전체 레코드 수를 반환한다`(testDB: TestDB) =
        runTest {
            withSuspendedEntityTable(testDB) {
                repository.countFromDb() shouldBeEqualTo getExistingIds().size.toLong()
            }
        }
}
