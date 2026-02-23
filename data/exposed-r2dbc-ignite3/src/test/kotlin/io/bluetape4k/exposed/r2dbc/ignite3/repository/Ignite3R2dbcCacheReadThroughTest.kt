package io.bluetape4k.exposed.r2dbc.ignite3.repository

import io.bluetape4k.exposed.r2dbc.ignite3.AbstractIgnite3R2dbcTest
import io.bluetape4k.exposed.r2dbc.ignite3.repository.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.ignite3.repository.UserSchema.getExistingId
import io.bluetape4k.exposed.r2dbc.ignite3.repository.UserSchema.getExistingIds
import io.bluetape4k.exposed.r2dbc.ignite3.repository.UserSchema.getNonExistentId
import io.bluetape4k.exposed.r2dbc.ignite3.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.ignite3.cache.IgniteNearCacheConfig
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
 * Apache Ignite 3.x 기반 [UserIgnite3R2dbcCacheRepository]의 Read-Through 캐시 동작을 검증하는 테스트입니다.
 *
 * [io.bluetape4k.testcontainers.storage.Ignite3Server.Launcher]를 통해
 * Docker 컨테이너로 Ignite 3.x 서버를 자동 실행하므로 별도 설치가 필요 없습니다.
 *
 * DB 접근은 Exposed R2DBC의 `suspendTransaction`을 사용합니다.
 *
 * **사전 요구사항**: Docker가 실행 중이어야 합니다.
 */
class Ignite3R2dbcCacheReadThroughTest: AbstractIgnite3R2dbcTest() {

    companion object: KLoggingChannel() {

        @JvmStatic
        fun enableDialects() = listOf(TestDB.H2)
    }

    private val config = IgniteNearCacheConfig(tableName = TEST_TABLE_NAME)

    private val repository by lazy {
        UserIgnite3R2dbcCacheRepository(igniteClient, config)
    }

    @BeforeEach
    fun setup() {
        // 테스트마다 Front Cache(Caffeine)를 초기화합니다
        repository.nearCache.clearFrontCache()
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `get으로 ID 조회 시 DB에서 읽어 Ignite3 캐시에 저장 후 반환한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val id = getExistingId()

            // DB에서 직접 조회
            val fromDb = repository.findByIdFromDb(id)
            fromDb.shouldNotBeNull()

            // 첫 조회: DB에서 읽어 Ignite3 캐시에 저장
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
    fun `findAll로 전체 엔티티를 DB에서 조회하고 Ignite3 캐시에 저장한다`(testDB: TestDB) = runTest {
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
    fun `invalidateFrontCache으로 Front Cache만 초기화한다`(testDB: TestDB) = runTest {
        withUserTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 로드
            repository.getAll(ids)

            // Front Cache(Caffeine)만 초기화
            repository.invalidateFrontCache()

            // Back Cache(Ignite3)에서 다시 조회 가능해야 함
            val entity = repository.get(ids.first())
            entity.shouldNotBeNull()
        }
    }
}
