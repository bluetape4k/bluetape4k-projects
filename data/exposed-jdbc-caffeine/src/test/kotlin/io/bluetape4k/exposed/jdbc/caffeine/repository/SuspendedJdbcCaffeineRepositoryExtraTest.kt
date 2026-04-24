package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.CredentialTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withSuspendedActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withSuspendedCredentialTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSuspendedJdbcCaffeineRepository
import io.bluetape4k.exposed.jdbc.caffeine.domain.CredentialSuspendedJdbcCaffeineRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Suspended JDBC Caffeine 레포지토리 추가 커버리지 테스트.
 *
 * 기존 시나리오 인터페이스에서 검증하지 않는 경로를 직접 테스트합니다:
 * - [AbstractSuspendedJdbcCaffeineRepository.countFromDb]
 * - [AbstractSuspendedJdbcCaffeineRepository.clear] 후 재조회
 * - [AbstractSuspendedJdbcCaffeineRepository.invalidateAll] (Collection 오버로드)
 * - Write-Behind [AbstractSuspendedJdbcCaffeineRepository.close] 시 남은 큐 flush 보장
 */
@Suppress("DEPRECATION")
class SuspendedJdbcCaffeineRepositoryExtraTest {

    companion object: KLogging()

    // -------------------------------------------------------------------------
    // countFromDb + clear + invalidateAll — AutoInc Actor
    // -------------------------------------------------------------------------

    @Nested
    inner class SuspendedActorCacheManagementTest: AbstractJdbcCaffeineTest() {

        // 파라미터화 테스트 실행마다 새 레포지토리를 생성해서 DB 인스턴스 간 캐시 오염을 방지한다
        private fun newRepository() = ActorSuspendedJdbcCaffeineRepository(
            LocalCacheConfig(keyPrefix = "jdbc:caffeine:s-extra:actor", writeMode = CacheWriteMode.READ_ONLY)
        )

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `countFromDb - DB 직접 카운트를 반환한다`(testDB: TestDB) = runTest(timeout = 30.seconds) {
            val repository = newRepository()
            withSuspendedActorTable(testDB) {
                // countFromDb()는 캐시를 우회해 DB에서 직접 집계해야 한다
                val count = repository.countFromDb()
                count shouldBeEqualTo ActorTable.selectAll().count()
                count shouldBeGreaterThan 0L
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `clear - 캐시를 비우면 다음 get은 DB에서 다시 로드된다`(testDB: TestDB) = runTest(timeout = 30.seconds) {
            // DB 인스턴스마다 독립된 레포지토리를 사용해야 캐시 오염을 방지할 수 있다
            val repository = newRepository()
            withSuspendedActorTable(testDB) {
                val id = newSuspendedTransaction(Dispatchers.IO) {
                    ActorTable.select(ActorTable.id).limit(1).first()[ActorTable.id].value
                }

                // 캐시에 적재
                val first = repository.get(id)
                first.shouldNotBeNull()

                // 캐시 전체 비우기
                repository.clear()

                // 내부 Caffeine 캐시에서 직접 조회하면 없어야 한다
                repository.cache.getIfPresent(id.toString()).shouldBeNull()

                // get()으로 재조회하면 Read-Through로 다시 로드된다
                val reloaded = repository.get(id)
                reloaded.shouldNotBeNull()
                // clear 후 재로드하면 동일한 DB 행이므로 같은 값이어야 한다
                reloaded shouldBeEqualTo first
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `invalidateAll - 여러 ID를 한 번에 캐시에서 제거한다`(testDB: TestDB) = runTest(timeout = 30.seconds) {
            val repository = newRepository()
            withSuspendedActorTable(testDB) {
                val ids = newSuspendedTransaction(Dispatchers.IO) {
                    ActorTable.select(ActorTable.id).map { it[ActorTable.id].value }
                }

                // 모두 캐시에 로드
                ids.forEach { repository.get(it) }

                // invalidateAll로 일괄 제거
                repository.invalidateAll(ids)

                // 캐시에서 모두 사라졌는지 확인
                ids.forEach { id ->
                    repository.cache.getIfPresent(id.toString()).shouldBeNull()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // countFromDb + clear + invalidateAll — Client-gen UUID Credential
    // -------------------------------------------------------------------------

    @Nested
    inner class SuspendedCredentialCacheManagementTest: AbstractJdbcCaffeineTest() {

        // 파라미터화 테스트 실행마다 새 레포지토리를 생성해서 DB 인스턴스 간 캐시 오염을 방지한다
        private fun newRepository() = CredentialSuspendedJdbcCaffeineRepository(
            LocalCacheConfig(keyPrefix = "jdbc:caffeine:s-extra:credential", writeMode = CacheWriteMode.READ_ONLY)
        )

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `countFromDb - UUID 테이블 DB 직접 카운트를 반환한다`(testDB: TestDB) = runTest(timeout = 30.seconds) {
            val repository = newRepository()
            withSuspendedCredentialTable(testDB) {
                val count = repository.countFromDb()
                count shouldBeEqualTo CredentialTable.selectAll().count()
                count shouldBeGreaterThan 0L
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `clear - UUID 테이블 캐시를 비우면 다음 get은 DB에서 다시 로드된다`(testDB: TestDB) = runTest(timeout = 30.seconds) {
            val repository = newRepository()
            withSuspendedCredentialTable(testDB) {
                val id = newSuspendedTransaction(Dispatchers.IO) {
                    CredentialTable.select(CredentialTable.id).limit(1).first()[CredentialTable.id].value
                }

                val first = repository.get(id)
                first.shouldNotBeNull()

                repository.clear()
                repository.cache.getIfPresent(id.toString()).shouldBeNull()

                val reloaded = repository.get(id)
                reloaded.shouldNotBeNull()
                reloaded shouldBeEqualTo first
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `invalidateAll - UUID 테이블 여러 ID를 한 번에 캐시에서 제거한다`(testDB: TestDB) = runTest(timeout = 30.seconds) {
            val repository = newRepository()
            withSuspendedCredentialTable(testDB) {
                val ids = newSuspendedTransaction(Dispatchers.IO) {
                    CredentialTable.select(CredentialTable.id).map { it[CredentialTable.id].value }
                }

                ids.forEach { repository.get(it) }

                repository.invalidateAll(ids)

                ids.forEach { id ->
                    repository.cache.getIfPresent(id.toString()).shouldBeNull()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Write-Behind close() flush 보장 테스트 — Client-gen UUID (non-AutoInc)
    // -------------------------------------------------------------------------

    @Nested
    inner class SuspendedWriteBehindCloseFlushTest: AbstractJdbcCaffeineTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `close - Write-Behind 종료 시 큐에 남은 항목이 DB에 flush된다`(testDB: TestDB) = runTest(timeout = 60.seconds) {
            // AutoInc 테이블은 새 엔티티를 DB에 삽입하지 않으므로 UUID 테이블로 검증
            val config = LocalCacheConfig(
                keyPrefix = "jdbc:caffeine:s-extra:wb-close",
                writeMode = CacheWriteMode.WRITE_BEHIND,
                writeBehindBatchSize = 100,
                writeBehindQueueCapacity = 10_000,
            )
            val repository = CredentialSuspendedJdbcCaffeineRepository(config)

            // autoInc면 건너뜀 (UUID 테이블이므로 항상 통과)
            Assumptions.assumeTrue(repository.table.id.autoIncColumnType == null) {
                "AutoInc 테이블은 Write-Behind 신규 삽입을 지원하지 않아 건너뜁니다"
            }

            val newEntities = List(10) { ActorSchema.newCredentialRecord() }
            val newMap = newEntities.associateBy { it.id }
            var prevCount: Long = 0

            // dropTables=false: statement 완료 후 테이블 유지 → statement 바깥에서 count 조회 가능
            // 트랜잭션 격리(REPEATABLE READ) 문제를 피하기 위해 count 조회를 statement 바깥(독립 트랜잭션)에서 수행한다.
            withTables(testDB, CredentialTable, dropTables = false) {
                repeat(3) {
                    CredentialTable.insert {
                        it[loginId] = faker.internet().domainWord() + "_swb_close_$it"
                        it[email] = "test_s_$it@example.com"
                        it[lastLoginAt] = Instant.now().minusSeconds(3600)
                    }
                }
                commit()

                prevCount = CredentialTable.selectAll().count()

                // 새 엔티티 10개를 Write-Behind 큐에 넣음
                // withTables는 suspend 미지원이므로 runBlocking으로 suspend 호출 감쌈
                runBlocking { repository.putAll(newMap) }

                // close() 호출 → 큐 드레인 후 종료 (flushBatch가 별도 transaction으로 커밋)
                repository.close()
            }

            // DB에 모두 반영됐는지 독립 트랜잭션으로 확인 (격리 수준 문제 우회)
            val newCount = transaction { CredentialTable.selectAll().count() }
            newCount shouldBeEqualTo prevCount + newEntities.size
        }
    }
}
