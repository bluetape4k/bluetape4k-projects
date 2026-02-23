package io.bluetape4k.exposed.ignite.repository

import io.bluetape4k.exposed.ignite.AbstractIgniteTest
import io.bluetape4k.exposed.ignite.repository.UserSchema.UserTable
import io.bluetape4k.exposed.ignite.repository.UserSchema.getExistingId
import io.bluetape4k.exposed.ignite.repository.UserSchema.getExistingIds
import io.bluetape4k.exposed.ignite.repository.UserSchema.getNonExistentId
import io.bluetape4k.exposed.ignite.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.ignite.cache.IgniteNearCacheConfig
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

/**
 * Apache Ignite 2.x 기반 [UserIgniteCacheRepository]의 Read-Through 캐시 동작을 검증하는 테스트입니다.
 *
 * 임베디드 Ignite 2.x 서버를 시작하고 씬 클라이언트로 연결하여 테스트합니다.
 */
class IgniteCacheReadThroughTest: AbstractIgniteTest() {

    companion object: KLogging() {

        @JvmStatic
        fun enableDialects() = listOf(TestDB.H2)
    }

    /**
     * 테스트마다 고유한 캐시 이름을 사용합니다.
     */
    private val config = IgniteNearCacheConfig(cacheName = "test-users-${System.nanoTime()}")

    private val repository by lazy {
        UserIgniteCacheRepository(igniteClient, config)
    }

    @BeforeEach
    fun setup() {
        // 테스트마다 Front Cache(Caffeine)를 초기화합니다
        repository.nearCache.clearFrontCache()
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `get으로 ID 조회 시 DB에서 읽어 캐시에 저장 후 반환한다`(testDB: TestDB) {
        withUserTable(testDB) {
            val id = getExistingId()

            // DB에서 직접 조회
            val fromDb = repository.findByIdFromDb(id)
            fromDb.shouldNotBeNull()

            // 첫 조회: DB에서 읽어 캐시에 저장
            val fromCache = repository.get(id)
            fromCache.shouldNotBeNull()
            fromCache shouldBeEqualTo fromDb

            // 캐시에 존재 확인
            repository.exists(id).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `존재하지 않는 ID 조회 시 null을 반환한다`(testDB: TestDB) {
        withUserTable(testDB) {
            val nonExistentId = getNonExistentId()

            repository.findByIdFromDb(nonExistentId).shouldBeNull()
            repository.get(nonExistentId).shouldBeNull()
            repository.exists(nonExistentId).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll로 전체 엔티티를 DB에서 조회하고 캐시에 저장한다`(testDB: TestDB) {
        withUserTable(testDB) {
            val entities = repository.findAll()
            entities.shouldNotBeEmpty()

            val dbCount = transaction {
                UserTable.selectAll().count().toInt()
            }
            entities.size shouldBeEqualTo dbCount
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `getAll로 여러 ID를 배치 조회한다`(testDB: TestDB) {
        withUserTable(testDB) {
            val ids = getExistingIds() + getNonExistentId()

            val entities = repository.getAll(ids, batchSize = 2)
            entities.shouldNotBeEmpty()
            // 존재하지 않는 ID는 결과에 포함되지 않음
            entities.size shouldBeEqualTo ids.size - 1
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `getAll에 빈 목록을 전달하면 빈 결과를 반환한다`(testDB: TestDB) {
        withUserTable(testDB) {
            repository.getAll(emptyList()).shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `getAll에 batchSize가 0이면 예외가 발생한다`(testDB: TestDB) {
        withUserTable(testDB) {
            assertFailsWith<IllegalArgumentException> {
                repository.getAll(getExistingIds(), batchSize = 0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `put으로 엔티티를 캐시에 저장하고 get으로 조회한다`(testDB: TestDB) {
        withUserTable(testDB) {
            val id = getExistingId()
            val entity = repository.findByIdFromDb(id)
            entity.shouldNotBeNull()

            // 캐시에 명시적으로 저장
            repository.put(entity)

            // 캐시에서 조회
            val cached = repository.get(id)
            cached.shouldNotBeNull()
            cached shouldBeEqualTo entity
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `invalidate으로 캐시에서 항목을 제거한다`(testDB: TestDB) {
        withUserTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 로드
            repository.getAll(ids)

            // 캐시에서 제거 (DB는 변경 없음)
            repository.invalidate(*ids.toTypedArray())

            // DB에서 다시 조회 가능해야 함
            val reloaded = repository.get(ids.first())
            reloaded.shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `invalidateFrontCache으로 Front Cache만 초기화한다`(testDB: TestDB) {
        withUserTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 로드
            repository.getAll(ids)

            // Front Cache(Caffeine)만 초기화
            repository.invalidateFrontCache()

            // Back Cache(Ignite)에서 다시 조회 가능해야 함
            val entity = repository.get(ids.first())
            entity.shouldNotBeNull()
        }
    }
}
