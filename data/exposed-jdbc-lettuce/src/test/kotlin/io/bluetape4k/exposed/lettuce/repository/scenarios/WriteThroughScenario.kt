package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.cache.scenarios.JdbcWriteThroughScenario
import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Write-through 캐시 전략 시나리오.
 *
 * - [JdbcWriteThroughScenario]를 확장하여 testFixtures의 @ParameterizedTest 시나리오도 포함
 * - put() 시 캐시와 DB를 동시에 갱신
 * - invalidate() 시 캐시와 DB를 모두 삭제
 */
interface WriteThroughScenario<ID : Any, E : java.io.Serializable> :
    JdbcWriteThroughScenario<ID, E>,
    CacheTestScenario<ID, E> {

    companion object : KLogging()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    fun updateEmail(entity: E): E

    // JdbcWriteThroughScenario.updateEntityEmail 을 updateEmail 로 위임
    override fun updateEntityEmail(entity: E): E = updateEmail(entity)

    // JdbcWriteThroughScenario.assertSameEntityWithoutUpdatedAt — 기본 equality 검사
    override fun assertSameEntityWithoutUpdatedAt(entity1: E, entity2: E) {
        // UserRecord에는 updatedAt이 없으므로 기본 equality 사용
        entity1 shouldBeEqualTo entity2
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - 캐시와 DB 모두에 반영된다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()
            val fromDb = repository.findByIdFromDb(id).shouldNotBeNull()

            val updated = updateEmail(fromDb)
            repository.put(id, updated)

            repository.get(id) shouldBeEqualTo updated
            repository.findByIdFromDb(id) shouldBeEqualTo updated
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - Map으로 일괄 저장 후 캐시와 DB 모두 반영된다`(testDB: TestDB) {
        withEntityTable(testDB) {
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
    fun `invalidate - 캐시에서만 삭제되고 DB는 유지된다`(testDB: TestDB) {
        withEntityTable(testDB) {
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
    fun `invalidateAll - 복수 ID를 캐시에서만 삭제하고 DB는 유지된다`(testDB: TestDB) {
        withEntityTable(testDB) {
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
    fun `findByIdFromDb - 캐시를 우회하고 DB에서 직접 조회한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()
            repository.findByIdFromDb(id).shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAllFromDb - 여러 ID로 DB에서 직접 조회한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds()
            val result = repository.findAllFromDb(ids)
            result.size shouldBeEqualTo ids.size
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countFromDb - DB 전체 레코드 수를 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val count = repository.countFromDb()
            count shouldBeEqualTo getExistingIds().size.toLong()
        }
    }
}
