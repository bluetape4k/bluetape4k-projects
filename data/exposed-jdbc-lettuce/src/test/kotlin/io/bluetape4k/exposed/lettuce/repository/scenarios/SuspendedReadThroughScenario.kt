package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Read-through 캐시 전략 suspend 시나리오.
 *
 * - 캐시 미스 시 DB에서 로드 후 캐시에 적재
 * - 캐시 삭제(delete) 시 DB에는 영향 없음 (ReadOnly 모드)
 */
interface SuspendedReadThroughScenario<ID : Comparable<ID>, E : Any> : SuspendedCacheTestScenario<ID, E> {
    companion object : KLoggingChannel()

    /** getNonExistentId()에 해당하는 엔티티를 생성해 반환한다 (DB에는 저장하지 않음) */
    suspend fun buildEntityForId(id: ID): E

    @Test
    fun `findById - 캐시 미스 시 DB에서 Read-through로 값을 로드한다`() =
        runTest {
            val id = getExistingId()
            val fromDb = repository.findByIdFromDb(id).shouldNotBeNull()

            repository.clearCache()
            val fromCache = repository.findById(id).shouldNotBeNull()
            fromCache shouldBeEqualTo fromDb
        }

    @Test
    fun `findById - DB에 없는 ID는 null을 반환한다`() =
        runTest {
            repository.findById(getNonExistentId()).shouldBeNull()
        }

    @Test
    fun `findAll - 여러 ID 일괄 조회 시 캐시 미스 키를 DB에서 Read-through한다`() =
        runTest {
            val ids = getExistingIds()
            val result = repository.findAll(ids)
            result.size shouldBeEqualTo ids.size
        }

    @Test
    fun `findAll - 존재하지 않는 ID는 결과에 포함되지 않는다`() =
        runTest {
            val ids = getExistingIds() + listOf(getNonExistentId())
            val result = repository.findAll(ids)
            result.size shouldBeEqualTo getExistingIds().size
        }

    @Test
    fun `clearCache - 캐시를 비운 후 재조회하면 DB에서 다시 Read-through한다`() =
        runTest {
            val id = getExistingId()
            repository.findById(id)
            repository.clearCache()

            repository.findById(id).shouldNotBeNull()
        }

    @Test
    fun `delete - 캐시에만 저장된 엔티티를 삭제하면 findById는 null을 반환한다`() =
        runTest {
            val id = getNonExistentId()
            repository.save(id, buildEntityForId(id))
            repository.delete(id)

            repository.findById(id).shouldBeNull()
        }
}
