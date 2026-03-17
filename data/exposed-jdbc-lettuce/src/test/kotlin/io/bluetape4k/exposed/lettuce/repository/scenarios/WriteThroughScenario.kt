package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Write-through 캐시 전략 시나리오.
 *
 * - save() 시 캐시와 DB를 동시에 갱신
 * - delete() 시 캐시와 DB를 모두 삭제
 */
interface WriteThroughScenario<ID : Comparable<ID>, E : Any> : CacheTestScenario<ID, E> {
    companion object : KLogging()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    fun updateEmail(entity: E): E

    @Test
    fun `save - 캐시와 DB 모두에 반영된다`() {
        val id = getExistingId()
        val fromDb = repository.findByIdFromDb(id).shouldNotBeNull()

        val updated = updateEmail(fromDb)
        repository.save(id, updated)

        repository.findById(id) shouldBeEqualTo updated
        repository.findByIdFromDb(id) shouldBeEqualTo updated
    }

    @Test
    fun `saveAll - Map으로 일괄 저장 후 캐시와 DB 모두 반영된다`() {
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
    fun `delete - 캐시와 DB 모두에서 삭제된다`() {
        val id = getExistingId()
        repository.save(id, repository.findByIdFromDb(id)!!)

        repository.delete(id)

        repository.findById(id).shouldBeNull()
        repository.findByIdFromDb(id).shouldBeNull()
    }

    @Test
    fun `deleteAll - 복수 ID를 한번에 삭제한다`() {
        val ids = getExistingIds()
        ids.forEach { id -> repository.save(id, repository.findByIdFromDb(id)!!) }

        repository.deleteAll(ids)

        ids.forEach { id ->
            repository.findById(id).shouldBeNull()
            repository.findByIdFromDb(id).shouldBeNull()
        }
    }

    @Test
    fun `findByIdFromDb - 캐시를 우회하고 DB에서 직접 조회한다`() {
        val id = getExistingId()
        repository.findByIdFromDb(id).shouldNotBeNull()
    }

    @Test
    fun `findAllFromDb - 여러 ID로 DB에서 직접 조회한다`() {
        val ids = getExistingIds()
        val result = repository.findAllFromDb(ids)
        result.size shouldBeEqualTo ids.size
    }

    @Test
    fun `countFromDb - DB 전체 레코드 수를 반환한다`() {
        val count = repository.countFromDb()
        count shouldBeEqualTo getExistingIds().size.toLong()
    }
}
