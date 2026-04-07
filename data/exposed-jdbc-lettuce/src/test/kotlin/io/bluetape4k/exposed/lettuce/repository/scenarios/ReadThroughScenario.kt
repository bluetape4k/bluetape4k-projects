package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.scenarios.JdbcReadThroughScenario
import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Read-through 캐시 전략 시나리오.
 *
 * - [JdbcReadThroughScenario]를 확장하여 testFixtures의 @ParameterizedTest 시나리오도 포함
 * - 캐시 미스 시 DB에서 로드 후 캐시에 적재
 * - 캐시 삭제(invalidate) 시 DB에는 영향 없음 (ReadOnly 모드)
 */
interface ReadThroughScenario<ID: Any, E: java.io.Serializable>:
    JdbcReadThroughScenario<ID, E>,
    CacheTestScenario<ID, E> {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get - 캐시 미스 시 DB에서 Read-through로 값을 로드한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()

            val fromDb = repository.findByIdFromDb(id)
            fromDb.shouldNotBeNull()

            repository.clear()
            val fromCache = repository.get(id)
            fromCache.shouldNotBeNull()
            fromCache shouldBeEqualTo fromDb
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get - DB에 없는 ID는 null을 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            repository.get(getNonExistentId()).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAll - 여러 ID를 일괄 조회하며 캐시 미스 키는 DB에서 Read-through한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds()
            val result = repository.getAll(ids)
            result.size shouldBeEqualTo ids.size
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAll - 존재하지 않는 ID는 결과에 포함되지 않는다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds() + listOf(getNonExistentId())
            val result = repository.getAll(ids)
            result.size shouldBeEqualTo getExistingIds().size
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `clear - 캐시를 비운 후 재조회하면 DB에서 다시 Read-through한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()
            repository.get(id) // 캐시에 적재
            repository.clear()

            val found = repository.get(id)
            found.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidate - 캐시에만 저장된 엔티티를 삭제하면 get은 null을 반환한다`(testDB: TestDB) {
        // WRITE_THROUGH/WRITE_BEHIND 모드에서는 put()이 DB에도 쓰므로 이 시나리오가 성립하지 않음
        Assumptions.assumeTrue(cacheWriteMode == CacheWriteMode.READ_ONLY) {
            "READ_ONLY 모드에서만 유효: WRITE_THROUGH/WRITE_BEHIND에서는 put()이 DB에도 기록됨"
        }
        withEntityTable(testDB) {
            val id = getNonExistentId()
            val entity = buildEntityForId(id)
            repository.put(id, entity) // READ_ONLY: Redis에만 저장

            repository.invalidate(id)

            repository.get(id).shouldBeNull()
        }
    }

    /**
     * getNonExistentId()에 해당하는 엔티티를 생성해 반환한다 (DB에는 저장하지 않음).
     * 구현 클래스에서 override한다.
     */
    fun buildEntityForId(id: ID): E
}
