package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserCredentialSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserCredentialSchema.withUserTables
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UserCredentialReadWriteThroughTest: AbstractRedissonTest() {

    companion object: KLogging()

    private val userRepository by lazy {
        UserCredentialCachedRepository(redissonClient, config = RedisCacheConfig.READ_WRITE_THROUGH)
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
    fun `Write Through 로 저장하면 DB와 캐시에 모두 저장된다`(testDB: TestDB) {
        withUserTables(testDB) {
            val newUserCredential = UserCredentialSchema.newUserCredential()

            // Write Through로 저장 (캐시 -> DB 모두 저장)
            userRepository.save(newUserCredential)

            // 캐시에서 조회
            val fromCache = userRepository.findById(newUserCredential.id)
            fromCache shouldBeEqualTo newUserCredential

            val updated = fromCache.copy(loginId = "updated")
            userRepository.save(updated)

            val fromCache2 = userRepository.findById(newUserCredential.id)
            fromCache2.loginId shouldBeEqualTo updated.loginId

            val fromDBUpdated = UserCredentialSchema.findUserCredentialById(updated.id)!!
            fromDBUpdated shouldBeEqualTo fromCache2.copy(updatedAt = fromDBUpdated.updatedAt)

            /**
             * 캐시를 비우면, [RedisCacheConfig.deleteFromDbOnInvalidate] 설정에 따라 DB에서도 삭제할 수 있습니다. (기본은 DB도 삭제됩니다)
             */
            log.debug { "캐시를 비우면 MapWriter 의 delete 함수가 호출된다, (DB 삭제를 하고 싶지 않다면 RedisCacheConfig.deleteFromDbOnInvalidate=false)" }
            userRepository.invalidate(newUserCredential.id)
            val fromDB = UserCredentialSchema.findUserCredentialById(newUserCredential.id)
            fromDB.shouldBeNull()
        }
    }
}
