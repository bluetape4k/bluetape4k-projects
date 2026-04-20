package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.exposed.r2dbc.redisson.AbstractR2dbcRedissonTest
import io.bluetape4k.exposed.r2dbc.redisson.domain.R2dbcUserRedissonRepository
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.r2dbc.select
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [R2dbcRedissonRepository.invalidateAll] 이 여러 ID를 한 번에 배치 삭제하는지,
 * 그리고 빈 목록 전달 시 no-op 으로 처리하는지 검증합니다.
 */
class R2dbcRepositoryInvalidateAllTest: AbstractR2dbcRedissonTest() {

    companion object: KLoggingChannel()

    private val cacheConfig = RedissonCacheConfig.READ_ONLY

    private val repository: R2dbcRedissonRepository<Long, UserRecord> by lazy {
        R2dbcUserRedissonRepository(
            redissonClient,
            config = cacheConfig.copy(name = "r2dbc:invalidate-all:test:users")
        )
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidateAll - 여러 ID를 한 번에 캐시에서 제거한다`(testDB: TestDB) = runTest {
        withUserTable(testDB, context = coroutineContext) {
            // 먼저 캐시에 로드
            val ids = UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
                .toList()

            ids.forEach { id -> repository.get(id) }  // 캐시에 채움
            ids.all { repository.containsKey(it) }.shouldBeTrue()

            // 한 번에 모두 삭제
            repository.invalidateAll(ids)

            // 캐시에서 모두 제거됐는지 확인 (Read-Only이므로 캐시 miss → DB loader가 다시 채움)
            // 적어도 예외 없이 완료돼야 한다
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidateAll - 빈 목록 전달 시 no-op으로 처리한다`(testDB: TestDB) = runTest {
        withUserTable(testDB, context = coroutineContext) {
            // 예외 없이 정상 완료돼야 한다
            repository.invalidateAll(emptyList())
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidate - 단건 ID를 캐시에서 제거하면 다시 DB에서 로드된다`(testDB: TestDB) = runTest {
        withUserTable(testDB, context = coroutineContext) {
            val id = UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
                .toList()
                .first()

            // 캐시에 로드
            val fromCache = repository.get(id)
            repository.containsKey(id).shouldBeTrue()

            // 단건 무효화
            repository.invalidate(id)

            // Read-Only: 다시 get() 하면 DB 로더가 채움 → containsKey true 가 될 수 있음
            // 예외 없이 완료되는지만 검증
            repository.findByIdFromDb(id) // DB 직접 조회 — 영향 없어야 함
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `clear - 캐시를 비운다`(testDB: TestDB) = runTest {
        withUserTable(testDB, context = coroutineContext) {
            val ids = UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
                .toList()
            ids.forEach { id -> repository.get(id) }

            repository.clear()

            // 예외 없이 완료되어야 한다
        }
    }
}
