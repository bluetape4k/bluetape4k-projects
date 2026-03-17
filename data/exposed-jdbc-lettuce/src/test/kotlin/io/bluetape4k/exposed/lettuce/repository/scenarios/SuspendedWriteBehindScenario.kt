package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Write-behind 캐시 전략 suspend 시나리오.
 *
 * - save() 시 캐시에는 즉시 반영, DB는 비동기로 적재
 */
interface SuspendedWriteBehindScenario<ID : Comparable<ID>, E : Any> : SuspendedCacheTestScenario<ID, E> {
    companion object : KLoggingChannel()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    suspend fun updateEmail(entity: E): E

    @Test
    fun `save - WRITE_BEHIND 저장 후 캐시에는 즉시 반영된다`() =
        runTest {
            val id = getExistingId()
            val entity = repository.findByIdFromDb(id).shouldNotBeNull()
            val updated = updateEmail(entity)
            repository.save(id, updated)

            repository.findById(id) shouldBeEqualTo updated
        }

    @Test
    fun `save - WRITE_BEHIND flush 주기 후 DB에 반영된다`() =
        runTest(timeout = 10.seconds) {
            val id = getExistingId()
            val entity = repository.findByIdFromDb(id).shouldNotBeNull()
            val updated = updateEmail(entity)
            repository.save(id, updated)

            val deadline = System.currentTimeMillis() + 5_000L
            while (repository.findByIdFromDb(id) != updated && System.currentTimeMillis() < deadline) {
                delay(100.milliseconds)
            }

            repository.findByIdFromDb(id) shouldBeEqualTo updated
        }

    @Test
    fun `saveAll - 여러 레코드를 배치로 비동기 적재한다`() =
        runTest(timeout = 10.seconds) {
            val ids = getExistingIds()
            val entities =
                ids.associateWith { id ->
                    updateEmail(repository.findByIdFromDb(id)!!)
                }
            repository.saveAll(entities)

            val deadline = System.currentTimeMillis() + 5_000L
            while (
                entities.any { (id, e) -> repository.findByIdFromDb(id) != e } &&
                System.currentTimeMillis() < deadline
            ) {
                delay(100.milliseconds)
            }

            entities.forEach { (id, expected) ->
                repository.findByIdFromDb(id) shouldBeEqualTo expected
            }
        }
}
