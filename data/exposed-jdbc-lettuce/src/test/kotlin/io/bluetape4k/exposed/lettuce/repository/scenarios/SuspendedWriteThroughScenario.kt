package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Write-through 캐시 전략 suspend 시나리오.
 *
 * - save() 시 캐시와 DB를 동시에 갱신
 * - delete() 시 캐시와 DB를 모두 삭제
 */
interface SuspendedWriteThroughScenario<ID : Comparable<ID>, E : Any> : SuspendedCacheTestScenario<ID, E> {
    companion object : KLoggingChannel()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    suspend fun updateEmail(entity: E): E

    @Test
    fun `save - 캐시와 DB 모두에 반영된다`() =
        runTest {
            val id = getExistingId()
            val entity = repository.findByIdFromDb(id).shouldNotBeNull()
            val updated = updateEmail(entity)
            repository.save(id, updated)

            repository.findById(id) shouldBeEqualTo updated
            repository.findByIdFromDb(id) shouldBeEqualTo updated
        }

    @Test
    fun `saveAll - Map 일괄 저장 후 캐시와 DB 모두 반영된다`() =
        runTest {
            val ids = getExistingIds()
            val entities = repository.findAll(ids)
            val updated = entities.mapValues { (_, v) -> updateEmail(v) }
            repository.saveAll(updated)

            updated.forEach { (id, entity) ->
                repository.findById(id) shouldBeEqualTo entity
                repository.findByIdFromDb(id) shouldBeEqualTo entity
            }
        }

    @Test
    fun `delete - 캐시와 DB 모두에서 삭제된다`() =
        runTest {
            val id = getExistingId()
            repository.save(id, repository.findByIdFromDb(id)!!)
            repository.delete(id)

            repository.findById(id).shouldBeNull()
            repository.findByIdFromDb(id).shouldBeNull()
        }

    @Test
    fun `deleteAll - 복수 ID를 한번에 삭제한다`() =
        runTest {
            val ids = getExistingIds()
            ids.forEach { id -> repository.save(id, repository.findByIdFromDb(id)!!) }
            repository.deleteAll(ids)

            ids.forEach { id ->
                repository.findById(id).shouldBeNull()
                repository.findByIdFromDb(id).shouldBeNull()
            }
        }

    @Test
    fun `countFromDb - DB 전체 레코드 수를 반환한다`() =
        runTest {
            repository.countFromDb() shouldBeEqualTo getExistingIds().size.toLong()
        }
}
