package io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios

import io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios.R2DbcLettuceJCacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Read-through 캐시 전략 R2DBC Lettuce 시나리오.
 *
 * - 캐시 미스 시 DB에서 로드 후 Redis에 적재
 * - delete 시 Redis에서만 삭제 (Read-only 모드에서 DB 영향 없음)
 */
interface R2dbcLettuceReadThroughScenario<ID: Any, E: Any>: R2DbcLettuceJCacheTestScenario<ID, E> {
    companion object: KLoggingChannel()

    /**
     * [getNonExistentId]에 해당하는 엔티티를 생성한다 (DB에는 저장하지 않음).
     * `delete` 시나리오에서 캐시만 존재하는 상태를 만들기 위해 사용한다.
     */
    suspend fun buildEntityForId(id: ID): E

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findById - 캐시 미스 시 DB에서 Read-through로 값을 로드한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                val fromDb = repository.findByIdFromDb(id).shouldNotBeNull()

                repository.clearCache()
                val fromCache = repository.findById(id).shouldNotBeNull()
                fromCache shouldBeEqualTo fromDb
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findById - DB에 없는 ID는 null을 반환한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                repository.findById(getNonExistentId()).shouldBeNull()
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll - 여러 ID 일괄 조회 시 캐시 미스 키를 DB에서 Read-through한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val ids = getExistingIds()
                val result = repository.findAll(ids)
                result.size shouldBeEqualTo ids.size
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll - 존재하지 않는 ID는 결과에 포함되지 않는다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val ids = getExistingIds() + listOf(getNonExistentId())
                val result = repository.findAll(ids)
                result.size shouldBeEqualTo getExistingIds().size
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `clearCache - 캐시를 비운 후 재조회하면 DB에서 다시 Read-through한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getExistingId()
                repository.findById(id)
                repository.clearCache()

                repository.findById(id).shouldNotBeNull()
            }
        }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete - 캐시 엔트리를 삭제하면 findById는 null을 반환한다`(testDB: TestDB) =
        runTest {
            withR2dbcEntityTable(testDB) {
                val id = getNonExistentId()
                repository.save(id, buildEntityForId(id))
                repository.delete(id)

                repository.findById(id).shouldBeNull()
            }
        }
}
