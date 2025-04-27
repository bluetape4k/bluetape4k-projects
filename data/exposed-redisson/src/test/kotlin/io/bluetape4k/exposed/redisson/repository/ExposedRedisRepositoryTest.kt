package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.CacheSchema.UserEntity
import io.bluetape4k.exposed.redisson.CacheSchema.UserTable
import io.bluetape4k.exposed.redisson.CacheSchema.toDTO
import io.bluetape4k.exposed.redisson.CacheSchema.withUserTables
import io.bluetape4k.exposed.redisson.ExposedRedisCacheConfig
import io.bluetape4k.exposed.tests.TestDB
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExposedRedisRepositoryTest: AbstractRedissonTest() {

    private val userRepository by lazy {
        UserRedisCachedCachedRepository(redissonClient, config = ExposedRedisCacheConfig.READ_THROUGH)
    }

    @BeforeEach
    fun setup() {
        // 다른 테스트에서 캐시에 사용한 값을 모두 삭제한다.
        userRepository.evictAll()
    }

    @Test
    fun `instancing repository`() {
        val repository = UserRedisCachedCachedRepository(redissonClient)
        repository.shouldNotBeNull()
        repository.table shouldBeEqualTo UserTable
    }

    @Test
    fun `Read through 로 id 로 조회한다`() {
        withUserTables(TestDB.H2) {
            // Read Through 로 캐시에서 조회
            val userFromCache = userRepository.findById(1L)
            // DB 에서 조회한 값
            val userFromDB = UserEntity.findById(1L)!!.toDTO()

            userFromCache shouldBeEqualTo userFromDB

            // 캐시에 ID=1 이 존재하는지 확인
            userRepository.existsById(1L).shouldBeTrue()
        }
    }

    @Test
    fun `엔티티 전체를 조회해서 캐시에 넣는다`() {
        withUserTables(TestDB.H2) {
            // UserTable 의 전체 레코드 수
            val userCount = UserEntity.all().count().toInt()

            // UserTable의 전체 레코드를 읽어 캐시에 넣는다
            userRepository.findAll() shouldHaveSize userCount

            // 모든 캐시를 삭제한다.
            userRepository.evictAll()

            // 다시 한번 UserTable의 전체 레코드를 읽어 캐시에 넣는다
            userRepository.findAll() shouldHaveSize userCount

            // 특정 조건의 레코드를 읽는다. ID(1, 3)은 캐시되어 있으므로, DB에서 읽지 않는다
            userRepository.findAll { UserTable.lastName eq "Bae" }.size shouldBeEqualTo 2
        }
    }

    @Test
    fun `Read Through 만 있는 경우, 캐시에 로드된 엔티티를 제거하면, 다시 로드한다`() {
        withUserTables(TestDB.H2) {
            // 캐시에서 ID=1인 엔티티를 읽는다 (기존에 없으므로 DB에서 읽는다)
            val user = userRepository.findById(1L)

            // 캐시에서 제거한다
            userRepository.cache.fastRemove(user.id)

            // 캐시에서 제거된 엔티티를 DB에서 읽어서 로드한다.
            val userFromCache = userRepository.findById(1L)

            userFromCache shouldBeEqualTo user
        }
    }
}
