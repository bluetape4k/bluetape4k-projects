package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Read-through 캐시 전략 시나리오.
 *
 * - 캐시 미스 시 DB에서 로드 후 캐시에 적재
 * - 캐시 삭제(delete) 시 DB에는 영향 없음 (ReadOnly 모드)
 */
interface ReadThroughScenario<ID: Any, E: Any>: CacheTestScenario<ID, E> {
    companion object : KLogging()

    @Test
    fun `findById - 캐시 미스 시 DB에서 Read-through로 값을 로드한다`() {
        val id = getExistingId()

        val fromDb = repository.findByIdFromDb(id)
        fromDb.shouldNotBeNull()

        repository.clearCache()
        val fromCache = repository.findById(id)
        fromCache.shouldNotBeNull()
        fromCache shouldBeEqualTo fromDb
    }

    @Test
    fun `findById - DB에 없는 ID는 null을 반환한다`() {
        repository.findById(getNonExistentId()).shouldBeNull()
    }

    @Test
    fun `findAll - 여러 ID를 일괄 조회하며 캐시 미스 키는 DB에서 Read-through한다`() {
        val ids = getExistingIds()
        val result = repository.findAll(ids)
        result.size shouldBeEqualTo ids.size
    }

    @Test
    fun `findAll - 존재하지 않는 ID는 결과에 포함되지 않는다`() {
        val ids = getExistingIds() + listOf(getNonExistentId())
        val result = repository.findAll(ids)
        result.size shouldBeEqualTo getExistingIds().size
    }

    @Test
    fun `clearCache - 캐시를 비운 후 재조회하면 DB에서 다시 Read-through한다`() {
        val id = getExistingId()
        repository.findById(id) // 캐시에 적재
        repository.clearCache()

        val found = repository.findById(id)
        found.shouldNotBeNull()
    }

    @Test
    fun `delete - 캐시에만 저장된 엔티티를 삭제하면 findById는 null을 반환한다`() {
        // READ_ONLY 모드에서 save()는 Redis에만 저장, DB에 쓰지 않음
        // → delete() 후 DB에도 없으므로 findById()는 null
        val id = getNonExistentId()
        val entity = buildEntityForId(id)
        repository.save(id, entity) // Redis에만 저장

        repository.delete(id)

        repository.findById(id).shouldBeNull()
    }

    /**
     * getNonExistentId()에 해당하는 엔티티를 생성해 반환한다 (DB에는 저장하지 않음).
     * 구현 클래스에서 override한다.
     */
    fun buildEntityForId(id: ID): E
}
