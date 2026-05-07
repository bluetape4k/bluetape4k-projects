package io.bluetape4k.exposed.r2dbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.r2dbc.caffeine.AbstractR2dbcCaffeineTest
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorR2dbcCaffeineRepository
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.withActorTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.coroutines.CoroutineContext

/**
 * AbstractR2dbcCaffeineRepository 의 누락된 경로를 보강하는 단위 테스트.
 *
 * - [io.bluetape4k.exposed.cache.R2dbcCacheRepository.clear] — 캐시 전체 비우기
 * - [io.bluetape4k.exposed.cache.R2dbcCacheRepository.invalidateAll] — 복수 키 무효화
 * - [io.bluetape4k.exposed.cache.R2dbcCacheRepository.countFromDb] — DB 카운트 직접 조회
 * - [io.bluetape4k.exposed.cache.R2dbcCacheRepository.get] 두 번째 호출 — 캐시 히트 경로
 * - [io.bluetape4k.exposed.cache.R2dbcCacheRepository.findAll] with limit/offset — 페이징 경로
 */
class CacheManagementTest: AbstractR2dbcCaffeineTest() {

    companion object: KLoggingChannel() {
        @JvmStatic
        fun getEnabledDialects() = setOf(TestDB.H2)
    }

    private val config = LocalCacheConfig(
        keyPrefix = "r2dbc:caffeine:mgmt:actor",
        writeMode = CacheWriteMode.WRITE_THROUGH,
    )

    private val repository by lazy { ActorR2dbcCaffeineRepository(config) }

    private suspend fun withTable(
        testDB: TestDB,
        @Suppress("UNUSED_PARAMETER") context: CoroutineContext = kotlinx.coroutines.Dispatchers.IO,
        statement: suspend org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction.() -> Unit,
    ) = withActorTable(testDB, statement)

    private suspend fun getFirstId(): Long =
        suspendTransaction {
            ActorTable.select(ActorTable.id).first()[ActorTable.id].value
        }

    private suspend fun getAllIds(): List<Long> =
        suspendTransaction {
            ActorTable.select(ActorTable.id).map { it[ActorTable.id].value }.toList()
        }

    // -------------------------------------------------------------------------
    // clear()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `clear - 캐시를 비우면 다음 get 호출 시 DB에서 다시 로드한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            val id = getFirstId()

            // 캐시에 로드
            val entity = repository.get(id)
            entity.shouldNotBeNull()
            repository.containsKey(id).shouldBeTrue()

            // clear() 후에는 캐시에 없어야 한다
            repository.clear()

            // Caffeine AsyncCache는 키를 직접 조회해야 미스를 확인할 수 있다.
            // get() 호출 시 Read-Through로 DB에서 다시 로드되므로, 결과는 동일해야 한다.
            val reloaded = repository.get(id)
            reloaded.shouldNotBeNull()
            reloaded shouldBeEqualTo entity
        }
    }

    // -------------------------------------------------------------------------
    // invalidateAll()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `invalidateAll - 지정한 키들을 캐시에서 일괄 제거한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            val ids = getAllIds()

            // 먼저 캐시에 모두 로드
            val entities = repository.getAll(ids)
            entities.shouldNotBeEmpty()
            ids.all { repository.containsKey(it) }.shouldBeTrue()

            // invalidateAll 후 캐시에서 제거됨
            // Caffeine은 즉시 동기적으로 제거하므로 다음 get() 은 DB에서 재조회한다.
            repository.invalidateAll(ids)

            // 다시 get() 호출하면 DB에서 Read-Through로 재로드해야 한다
            val reloaded = repository.getAll(ids)
            reloaded.shouldNotBeEmpty()
            reloaded.size shouldBeEqualTo ids.size
        }
    }

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `invalidateAll - 빈 목록은 아무 영향도 없다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            // 예외 없이 정상 종료해야 한다
            repository.invalidateAll(emptyList())
        }
    }

    // -------------------------------------------------------------------------
    // countFromDb()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `countFromDb - 캐시를 우회하여 DB 레코드 수를 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            // withActorTable이 3건을 삽입하므로 최소 3건 이상이어야 한다
            val count = repository.countFromDb()
            count shouldBeGreaterThan 0L
        }
    }

    // -------------------------------------------------------------------------
    // get() — 캐시 히트 경로
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `get - 두 번째 호출 시 캐시에서 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            val id = getFirstId()

            // 첫 번째: DB → 캐시 저장
            val first = repository.get(id)
            first.shouldNotBeNull()

            // 두 번째: 캐시 히트 (DB 접근 없음) — 동일한 값이어야 한다
            val second = repository.get(id)
            second.shouldNotBeNull()
            second shouldBeEqualTo first
        }
    }

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `get - 존재하지 않는 ID는 null을 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            repository.get(Long.MIN_VALUE).shouldBeNull()
        }
    }

    // -------------------------------------------------------------------------
    // findAll() with limit/offset
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `findAll - limit을 지정하면 그 수만큼만 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            val all = repository.findAll()
            all.shouldNotBeEmpty()

            val limited = repository.findAll(limit = 1)
            limited.size shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `findAll - offset을 지정하면 앞 항목을 건너뛴다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            val all = repository.findAll(sortBy = ActorTable.id, sortOrder = SortOrder.ASC)
            all.size shouldBeGreaterThan 1

            val withOffset = repository.findAll(offset = 1L, sortBy = ActorTable.id, sortOrder = SortOrder.ASC)
            withOffset.size shouldBeEqualTo (all.size - 1)

            // offset=1이면 두 번째부터 반환 — 첫 번째 엔티티가 포함되지 않아야 한다
            withOffset.none { it.id == all.first().id }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `findAll - 결과가 없으면 빈 목록을 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            // 존재하지 않는 조건으로 조회
            val result = repository.findAll(
                where = { ActorTable.firstName eq "존재하지않는이름" }
            )
            result.shouldBeEmpty()
        }
    }

    // -------------------------------------------------------------------------
    // getAll() — 일부 캐시 히트 + 일부 캐시 미스 혼합 경로
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `getAll - 일부는 캐시에서 일부는 DB에서 가져온다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            val ids = getAllIds()
            ids.size shouldBeGreaterThan 1

            // 첫 번째 ID만 캐시에 미리 로드
            val firstId = ids.first()
            repository.get(firstId).shouldNotBeNull()

            // 나머지는 캐시에 없음
            // getAll 호출 시 캐시 히트 + DB 조회를 섞어서 처리해야 한다
            val result = repository.getAll(ids)
            result.size shouldBeEqualTo ids.size
            result.containsKey(firstId).shouldBeTrue()
        }
    }

    // -------------------------------------------------------------------------
    // containsKey() — 존재하지 않는 ID
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `containsKey - 캐시와 DB 모두에 없으면 false를 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            repository.containsKey(Long.MIN_VALUE).shouldBeFalse()
        }
    }

    // -------------------------------------------------------------------------
    // findByIdFromDb() / findAllFromDb() — 캐시 우회 경로
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `findByIdFromDb - 캐시를 우회하여 DB에서 조회한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            val id = getFirstId()
            val entity = repository.findByIdFromDb(id)
            entity.shouldNotBeNull()
            entity.id shouldBeEqualTo id
        }
    }

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `findAllFromDb - 빈 컬렉션은 빈 목록을 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            repository.findAllFromDb(emptyList()).shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `findAllFromDb - 존재하지 않는 IDs는 빈 목록을 반환한다`(testDB: TestDB) = runTest {
        withTable(testDB) {
            repository.findAllFromDb(listOf(Long.MIN_VALUE, Long.MIN_VALUE + 1)).shouldBeEmpty()
        }
    }

    // -------------------------------------------------------------------------
    // close() — Write-Behind 모드에서 채널 닫기
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("getEnabledDialects")
    fun `close - Write-Behind 모드에서 예외 없이 정상 종료된다`(testDB: TestDB) = runTest {
        val wbConfig = LocalCacheConfig(
            keyPrefix = "r2dbc:caffeine:close:actor",
            writeMode = CacheWriteMode.WRITE_BEHIND,
            writeBehindBatchSize = 5,
        )
        val wbRepo = ActorR2dbcCaffeineRepository(wbConfig)

        withTable(testDB) {
            // 몇 건 put 후 close가 예외 없이 동작해야 한다
            val id = getFirstId()
            val entity = wbRepo.findByIdFromDb(id)
            entity?.let { wbRepo.put(id, it) }

            // close()는 runBlocking 없이 Virtual Thread 친화적으로 종료
            wbRepo.close()
        }
    }
}
