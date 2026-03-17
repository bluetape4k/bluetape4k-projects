package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * Write-behind 캐시 전략 시나리오.
 *
 * - save() 시 캐시에는 즉시 반영, DB는 비동기로 적재
 */
interface WriteBehindScenario<ID: Any, E: Any>: CacheTestScenario<ID, E> {
    companion object : KLogging()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    fun updateEmail(entity: E): E

    /** DB 반영 완료까지 폴링 대기 */
    fun awaitDbReflection(
        timeout: Long = 5_000L,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeout
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100L)
        }
    }

    @Test
    fun `save - WRITE_BEHIND 저장 후 캐시에는 즉시 반영된다`() {
        val id = getExistingId()
        val entity = repository.findByIdFromDb(id).shouldNotBeNull()

        val updated = updateEmail(entity)
        repository.save(id, updated)

        repository.findById(id) shouldBeEqualTo updated
    }

    @Test
    fun `save - WRITE_BEHIND flush 주기 후 DB에 반영된다`() {
        val id = getExistingId()
        val entity = repository.findByIdFromDb(id).shouldNotBeNull()

        val updated = updateEmail(entity)
        repository.save(id, updated)

        awaitDbReflection { repository.findByIdFromDb(id) == updated }

        repository.findByIdFromDb(id) shouldBeEqualTo updated
    }

    @Test
    fun `saveAll - 여러 레코드를 배치로 비동기 적재한다`() {
        val ids = getExistingIds()
        val entities =
            ids.associateWith { id ->
                updateEmail(repository.findByIdFromDb(id)!!)
            }
        repository.saveAll(entities)

        awaitDbReflection {
            entities.all { (id, expected) -> repository.findByIdFromDb(id) == expected }
        }

        entities.forEach { (id, expected) ->
            repository.findByIdFromDb(id) shouldBeEqualTo expected
        }
    }
}
