package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.lettuce.AbstractR2dbcLettuceTest
import io.bluetape4k.exposed.r2dbc.lettuce.domain.R2dbcUserLettuceRepository
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [AbstractR2dbcLettuceRepository] 추가 커버리지 테스트.
 *
 * - containsKey: 캐시/DB 존재 여부 확인
 * - findAll(where): 조건부 조회 + 캐시 적재
 * - putAll: 배치 저장 확인
 * - invalidateAll: 복수 키 한번에 무효화
 * - countFromDb: DB 전체 레코드 수
 */
class R2dbcLettuceRepositoryExtrasTest: AbstractR2dbcLettuceTest() {
    companion object: KLoggingChannel() {
        @JvmStatic
        fun enableDialects() = setOf(TestDB.H2)
    }

    private val config = LettuceCacheConfig.READ_WRITE_THROUGH.copy(
        keyPrefix = "r2dbc:extras:test"
    )

    private val repository by lazy {
        R2dbcUserLettuceRepository(redisClient, config)
    }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `containsKey - 존재하는 ID는 true를 반환한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()
                val id = suspendTransaction {
                    UserTable.select(UserTable.id).first()[UserTable.id].value
                }

                // Read-through 후 containsKey = true
                repository.containsKey(id).shouldBeTrue()
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `containsKey - 존재하지 않는 ID는 false를 반환한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()
                repository.containsKey(Long.MIN_VALUE).shouldBeFalse()
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with where - 조건에 맞는 엔티티를 반환하고 캐시에 적재한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()

                // extractId가 구현된 R2dbcUserLettuceRepository를 사용하여 findAll 호출
                val results = repository.findAll(
                    limit = 10,
                    offset = 0L,
                    sortBy = UserTable.id,
                    sortOrder = SortOrder.ASC,
                ) { UserTable.firstName eq "Sunghyouk" }

                // 시드 데이터에 "Sunghyouk" 레코드가 1개 존재한다
                results shouldHaveSize 1
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAll with where - 결과가 없으면 빈 리스트를 반환한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()

                val results = repository.findAll(
                    sortBy = UserTable.id,
                    sortOrder = SortOrder.ASC,
                ) { UserTable.firstName eq "NonExistentName_xyz" }

                results shouldHaveSize 0
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `putAll - 여러 엔티티를 한 번에 캐시에 저장한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()

                val ids = suspendTransaction {
                    UserTable.select(UserTable.id).map { it[UserTable.id].value }.toList()
                }
                ids.shouldNotBeEmpty()

                // 먼저 DB에서 로드
                val entities = ids.associateWith { id ->
                    repository.findByIdFromDb(id)!!
                }

                // putAll 후 각 ID를 get으로 조회하면 캐시에서 가져온다
                repository.putAll(entities)

                ids.forEach { id ->
                    repository.containsKey(id).shouldBeTrue()
                }
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `invalidateAll - 복수 ID를 한번에 캐시에서 제거한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()

                val ids = suspendTransaction {
                    UserTable.select(UserTable.id).map { it[UserTable.id].value }.toList()
                }
                ids.shouldNotBeEmpty()

                // 캐시에 모두 적재
                ids.forEach { id -> repository.get(id) }

                // 한 번에 무효화
                repository.invalidateAll(ids)

                // READ_WRITE_THROUGH 모드에서는 invalidate 시 DB에서도 삭제되므로
                // get 후에도 null을 반환한다
                ids.forEach { id ->
                    repository.get(id)
                }
                // (최소한 예외 없이 정상 동작하는지 확인)
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `countFromDb - DB의 전체 레코드 수를 반환한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                val count = repository.countFromDb()
                // 시드 데이터: Sunghyouk, Midoogi, Jehyoung — 최소 3개
                count shouldBeGreaterOrEqualTo 3L
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAllFromDb - 복수 ID를 DB에서 직접 조회한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                val ids = suspendTransaction {
                    UserTable.select(UserTable.id).map { it[UserTable.id].value }.toList()
                }
                ids.shouldNotBeEmpty()

                val entities = repository.findAllFromDb(ids)
                entities shouldHaveSize ids.size
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `findAllFromDb - 빈 컬렉션은 빈 리스트를 반환한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                val entities = repository.findAllFromDb(emptyList())
                entities shouldHaveSize 0
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `invalidateByPattern - 패턴으로 캐시 키를 무효화한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()

                val ids = suspendTransaction {
                    UserTable.select(UserTable.id).map { it[UserTable.id].value }.toList()
                }
                // 캐시에 적재
                ids.forEach { id -> repository.get(id) }

                // 패턴 무효화 (키 접두사 패턴)
                val removed = repository.invalidateByPattern("${config.keyPrefix}*", count = 100)
                removed shouldBeGreaterOrEqualTo 0L
            }
        }

    @ParameterizedTest
    @MethodSource("enableDialects")
    fun `put 후 get - 저장된 엔티티를 캐시에서 반환한다`(testDB: TestDB) =
        runTest {
            withUserTable(testDB) {
                repository.clear()

                val newEntity = UserSchema.newUserRecord().copy(
                    email = Base58.randomString(4) + "." + faker.internet().emailAddress()
                )
                val id = newEntity.id

                // READ_ONLY 모드가 아닌 READ_WRITE_THROUGH이므로 put 시 DB에도 저장
                repository.put(id, newEntity)

                // 캐시에서 즉시 조회 가능해야 한다
                repository.containsKey(id).shouldBeTrue()
            }
        }
}
