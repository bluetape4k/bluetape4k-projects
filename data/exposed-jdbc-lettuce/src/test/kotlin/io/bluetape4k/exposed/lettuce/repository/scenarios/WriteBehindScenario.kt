package io.bluetape4k.exposed.lettuce.repository.scenarios

import io.bluetape4k.exposed.cache.scenarios.JdbcWriteBehindScenario
import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration

/**
 * Write-behind 캐시 전략 시나리오.
 *
 * - [JdbcWriteBehindScenario]를 확장하여 testFixtures의 @ParameterizedTest 시나리오도 포함
 * - put() 시 캐시에는 즉시 반영, DB는 비동기로 적재
 */
interface WriteBehindScenario<ID: Any, E: java.io.Serializable>:
    JdbcWriteBehindScenario<ID, E>,
    CacheTestScenario<ID, E> {

    companion object: KLogging()

    /** 기존 엔티티의 이메일을 수정한 복사본을 반환한다 */
    fun updateEmail(entity: E): E

    // JdbcWriteBehindScenario.createNewEntity 기본 구현은 subclass에서 override
    override fun createNewEntity(): E

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - WRITE_BEHIND 저장 후 캐시에는 즉시 반영된다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()
            val entity = repository.findByIdFromDb(id).shouldNotBeNull()

            val updated = updateEmail(entity)
            repository.put(id, updated)

            repository.get(id) shouldBeEqualTo updated
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - WRITE_BEHIND flush 주기 후 DB에 반영된다`(testDB: TestDB) {
        // MySQL/MariaDB의 REPEATABLE READ 격리 수준은 외부 트랜잭션에서 커밋된 데이터를 볼 수 없어 스킵
        Assumptions.assumeTrue(testDB !in TestDB.ALL_MYSQL_MARIADB) {
            "${testDB}은 REPEATABLE READ 격리 수준으로 Write-Behind DB 가시성 테스트 불가"
        }
        withEntityTable(testDB) {
            val id = getExistingId()
            val entity = repository.findByIdFromDb(id).shouldNotBeNull()

            val updated = updateEmail(entity)
            repository.put(id, updated)

            await
                .atMost(Duration.ofSeconds(5))
                .withPollInterval(Duration.ofMillis(100))
                .until { repository.findByIdFromDb(id) == updated }

            repository.findByIdFromDb(id) shouldBeEqualTo updated
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - 여러 레코드를 배치로 비동기 적재한다`(testDB: TestDB) {
        // MySQL/MariaDB의 REPEATABLE READ 격리 수준은 외부 트랜잭션에서 커밋된 데이터를 볼 수 없어 스킵
        Assumptions.assumeTrue(testDB !in TestDB.ALL_MYSQL_MARIADB) {
            "${testDB}은 REPEATABLE READ 격리 수준으로 Write-Behind DB 가시성 테스트 불가"
        }
        withEntityTable(testDB) {
            val ids = getExistingIds()
            val entities =
                ids.associateWith { id ->
                    updateEmail(repository.findByIdFromDb(id)!!)
                }
            repository.putAll(entities)

            await
                .atMost(Duration.ofSeconds(5))
                .withPollInterval(Duration.ofMillis(100))
                .until { entities.all { (id, expected) -> repository.findByIdFromDb(id) == expected } }

            entities.forEach { (id, expected) ->
                repository.findByIdFromDb(id) shouldBeEqualTo expected
            }
        }
    }
}
