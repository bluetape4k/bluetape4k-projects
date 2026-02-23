package io.bluetape4k.exposed.r2dbc.hazelcast.repository

import io.bluetape4k.exposed.r2dbc.hazelcast.AbstractHazelcastR2dbcTest
import io.bluetape4k.exposed.r2dbc.hazelcast.repository.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.hazelcast.repository.UserSchema.getExistingId
import io.bluetape4k.exposed.r2dbc.hazelcast.repository.UserSchema.getExistingIds
import io.bluetape4k.exposed.r2dbc.hazelcast.repository.UserSchema.getNonExistentId
import io.bluetape4k.exposed.r2dbc.hazelcast.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

/**
 * Hazelcast 기반 [UserHazelcastR2dbcCacheRepository]의 Read-Through 캐시 동작을 검증하는 테스트입니다.
 *
 * [io.bluetape4k.testcontainers.storage.HazelcastServer.Launcher]를 통해
 * Docker 컨테이너로 Hazelcast 서버를 자동 실행하므로 별도 설치가 필요 없습니다.
 *
 * DB 접근은 Exposed R2DBC의 `suspendTransaction`을 사용합니다.
 *
 * **사전 요구사항**: Docker가 실행 중이어야 합니다.
 */
class HazelcastR2dbcCacheReadThroughTest: AbstractHazelcastR2dbcTest() {

    companion object: KLoggingChannel() {

        @JvmStatic
        fun enableDialects() = listOf(TestDB.H2)
    }

    private val repository by lazy {
        UserHazelcastR2dbcCacheRepository(hazelcast)
    }

    @BeforeEach
    fun setup() {
        // 테스트마다 캐시를 초기화합니다
        repository.cache.clear()
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `get으로 ID 조회 시 DB에서 읽어 Hazelcast 캐시에 저장 후 반환한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val id = getExistingId()

            // DB에서 직접 조회
            val fromDb = repository.findByIdFromDb(id)
            fromDb.shouldNotBeNull()

            // 첫 조회: DB에서 읽어 Hazelcast 캐시에 저장
            val fromCache = repository.get(id)
            fromCache.shouldNotBeNull()
            fromCache shouldBeEqualTo fromDb

            // 캐시에 존재 확인
            repository.exists(id).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `존재하지 않는 ID 조회 시 null을 반환한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val nonExistentId = getNonExistentId()

            repository.findByIdFromDb(nonExistentId).shouldBeNull()
            repository.get(nonExistentId).shouldBeNull()
            repository.exists(nonExistentId).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll로 전체 엔티티를 DB에서 조회하고 Hazelcast 캐시에 저장한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val entities = repository.findAll()
            entities.shouldNotBeEmpty()

            val dbCount = suspendTransaction {
                UserTable.selectAll().count().toInt()
            }
            entities.size shouldBeEqualTo dbCount
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `getAll로 여러 ID를 배치 조회한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val ids = getExistingIds() + getNonExistentId()

            val entities = repository.getAll(ids, batchSize = 2)
            entities.shouldNotBeEmpty()
            entities.size shouldBeEqualTo ids.size - 1
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `getAll에 빈 목록을 전달하면 빈 결과를 반환한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            repository.getAll(emptyList()).shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `getAll에 batchSize가 0이면 예외가 발생한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            assertFailsWith<IllegalArgumentException> {
                repository.getAll(getExistingIds(), batchSize = 0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `put으로 엔티티를 캐시에 저장하고 get으로 조회한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val id = getExistingId()
            val entity = repository.findByIdFromDb(id)
            entity.shouldNotBeNull()

            repository.put(entity)

            val cached = repository.get(id)
            cached.shouldNotBeNull()
            cached shouldBeEqualTo entity
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `invalidate으로 캐시에서 항목을 제거한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 로드
            repository.getAll(ids)

            // 캐시에서 제거
            repository.invalidate(*ids.toTypedArray())

            // DB에서 다시 조회 가능해야 함
            val reloaded = repository.get(ids.first())
            reloaded.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `invalidateAll로 전체 캐시를 초기화한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 로드
            repository.getAll(ids)

            // 전체 캐시 초기화
            repository.invalidateAll()

            // DB에서 다시 조회 가능해야 함
            val entity = repository.get(ids.first())
            entity.shouldNotBeNull()
        }
    }
}
