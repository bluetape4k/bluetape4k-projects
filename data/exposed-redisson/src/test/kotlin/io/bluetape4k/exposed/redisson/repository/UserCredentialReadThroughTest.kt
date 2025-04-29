package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.toUserCredential
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserTables
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UserCredentialReadThroughTest: AbstractRedissonTest() {

    companion object: KLogging()

    private val userRepository by lazy {
        UserCredentialCachedRepository(redissonClient, config = RedisCacheConfig.READ_ONLY)
    }

    @BeforeEach
    fun setup() {
        // 다른 테스트에서 캐시에 사용한 값을 모두 삭제한다.
        userRepository.invalidateAll()
    }

    @Test
    fun `instancing repository`() {
        userRepository.shouldNotBeNull()
        userRepository.entityTable shouldBeEqualTo UserCredentialTable
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Read through 로 id 로 조회한다`(testDB: TestDB) {
        withUserTables(testDB) {
            // DB 에서 조회한 값
            val userFromDB = UserCredentialTable
                .selectAll()
                .where { UserCredentialTable.id eq 1L }
                .singleOrNull()
                ?.toUserCredential()

            // Read Through 로 캐시에서 조회
            val userFromCache = userRepository.findById(1L)
            userFromCache shouldBeEqualTo userFromDB

            // 캐시에 ID=1 이 존재하는지 확인
            userRepository.existsById(1L).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `엔티티 전체를 조회해서 캐시에 넣는다`(testDB: TestDB) {
        withUserTables(testDB) {
            // UserTable 의 전체 레코드 수
            val userCount = UserCredentialTable.selectAll().count().toInt()

            // UserTable의 전체 레코드를 읽어 캐시에 넣는다
            userRepository.findAll() shouldHaveSize userCount

            // 모든 캐시를 삭제한다.
            userRepository.invalidateAll()

            // 다시 한번 UserTable의 전체 레코드를 읽어 캐시에 넣는다
            userRepository.findAll() shouldHaveSize userCount

            // 특정 조건의 레코드를 읽는다. ID(1, 3)은 캐시되어 있으므로, DB에서 읽지 않는다
            userRepository.findAll { UserCredentialTable.loginId eq "debop" }.size shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Read Through 만 있는 경우, 캐시에 로드된 엔티티를 제거하면, 다시 로드한다`(testDB: TestDB) {
        withUserTables(testDB) {
            // 캐시에서 ID=1인 엔티티를 읽는다 (기존에 없으므로 DB에서 읽는다)
            val user = userRepository.findById(1L)

            // 캐시에서 제거한다
            userRepository.invalidate(user.id)

            // 캐시에서 제거된 엔티티를 DB에서 읽어서 로드한다.
            val userFromCache = userRepository.findById(1L)

            userFromCache shouldBeEqualTo user
        }
    }
}
