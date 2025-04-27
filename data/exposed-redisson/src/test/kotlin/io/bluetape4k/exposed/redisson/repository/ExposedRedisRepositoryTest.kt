package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.CacheSchema.UserEntity
import io.bluetape4k.exposed.redisson.CacheSchema.UserTable
import io.bluetape4k.exposed.redisson.CacheSchema.toDTO
import io.bluetape4k.exposed.redisson.CacheSchema.withUserTables
import io.bluetape4k.exposed.tests.TestDB
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExposedRedisRepositoryTest: AbstractRedissonTest() {

    private val userRepository by lazy {
        UserRedisCachedCachedRepository(redissonClient)
    }

    @BeforeEach
    fun setup() {
        userRepository.cache.clear()
    }

    @Test
    fun `instancing repository`() {
        val repository = UserRedisCachedCachedRepository(redissonClient)
        repository.shouldNotBeNull()
        repository.table shouldBeEqualTo UserTable
    }

    @Test
    fun `find by id with read through`() {
        withUserTables(TestDB.H2) {
            val userFromDB = UserEntity.findById(1L)!!.toDTO()
            val userFromCache = userRepository.findById(1L)
            userFromCache shouldBeEqualTo userFromDB
        }
    }

    @Test
    fun `load all entities to cache`() {
        withUserTables(TestDB.H2) {
            val entities = userRepository.findAll()
            entities.size shouldBeEqualTo 3

            userRepository.cache.clear()
            userRepository.findAll().size shouldBeEqualTo 3

            repeat(5) {
                userRepository.findAll { UserTable.lastName eq "Bae" }.size shouldBeEqualTo 2
            }
        }
    }
}
