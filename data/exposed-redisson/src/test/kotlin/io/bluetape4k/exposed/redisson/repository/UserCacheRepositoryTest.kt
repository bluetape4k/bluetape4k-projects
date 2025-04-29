package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UserCacheRepositoryTest {

    @Nested
    inner class ReadThroughOnly: AbstractRedissonTest() {
        private val userRepository =
            UserCacheRepository(
                redissonClient,
                "exposed:readonly:users",
                RedisCacheConfig.READ_ONLY
            )

        @BeforeEach
        fun setup() {
            // 다른 테스트에서 캐시에 사용한 값을 모두 삭제한다.
            userRepository.invalidateAll()
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `ID로 조회 시 DB에서 읽어서 캐시에 저장 후 반환한다`(testDB: TestDB) {
            withUserTable(testDB) {
                // DB 에서 조회한 값
                val userFromDB = userRepository.findFreshById(1L)

                // 캐시에서 조회한 값
                val userFromCache = userRepository.get(1L)
                userFromCache.shouldNotBeNull()
                userFromCache shouldBeEqualTo userFromDB

                userRepository.exists(1L).shouldBeTrue()
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `검색한 엔티티들을 캐시에 추가한 후 반환한다`(testDB: TestDB) {
            withUserTable(testDB) {
                // DB에서 조회한 UserDTO를 캐시에 저장한 후 반환한다.
                val usersFromCache = userRepository.findAll {
                    UserTable.lastName eq "Bae"
                }
                usersFromCache shouldHaveSize 2
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `모든 엔티티들을 캐시에 추가한 후 반환한다`(testDB: TestDB) {
            withUserTable(testDB) {
                // DB의 엔티티 수
                val entityCountInDB = UserTable.selectAll().count().toInt()

                // DB에서 조회한 모든 UserDTO를 캐시에 저장한 후 반환한다.
                val usersFromCache = userRepository.findAll()
                usersFromCache shouldHaveSize entityCountInDB
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `복수의 ID에 해당하는 엔티티를 가져온다`(testDB: TestDB) {
            withUserTable(testDB) {
                val ids = listOf(1L, 2L, 3L, 4L)  // 4L 은 없다
                val users = userRepository.getAllBatch(ids, batchSize = 2)
                users shouldHaveSize ids.size - 1
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `캐시 invalidate 후 다수 조회하면 DB에서 읽어온다`(testDB: TestDB) {
            withUserTable(testDB) {
                // 캐시에서 조회한 값
                val userFromCache = userRepository.get(1L)
                userFromCache.shouldNotBeNull()

                // 캐시에서 삭제한다.
                userRepository.invalidate(1L)

                // 캐시에 없으면, DB에서 다시 읽어온다
                userRepository.exists(1L).shouldBeTrue()

                // DB 에서 조회한 값
                val userFromDB = userRepository.findFreshById(1L)
                userFromCache shouldBeEqualTo userFromDB
            }
        }
    }

    @Nested
    inner class ReadWriteThough: AbstractRedissonTest() {
        /**
         * Read-Write Through 캐시를 사용합니다.
         * 캐시에 저장하면 DB에도 저장됩니다, 캐시에서 삭제하면 DB에서도 삭제됩니다.
         */
        private val userRepository by lazy {
            UserCacheRepository(
                redissonClient,
                "exposed:read-write:delete-db:users",
                RedisCacheConfig.READ_WRITE_THROUGH.copy(deleteFromDBOnInvalidate = true)
            )
        }


        @BeforeEach
        fun setup() {
            // WriteThrough 인 경우 캐시에서 삭제하면 DB에서도 삭제됩니다!!!
            userRepository.invalidateAll()
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `검색 결과를 캐시에 모두 저장하고, 반환합니다`(testDB: TestDB) {
            withUserTable(testDB) {
                // DB 조회 결과에 해당하는 엔티티를 캐시에 저장한 후 반환합니다.
                val users = userRepository.findAll { UserTable.lastName eq "Bae" }

                // 캐시에 존재하므로 모두 true를 반환합니다.
                users.all { userRepository.exists(it.id) }.shouldBeTrue()
            }
        }

        /**
         * UserTable 이 AutoIncremented ID 이므로, UserDTO로는 INSERT를 못하고, Update 만 가능합니다.
         */
        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `캐시에 저장하면 DB에도 반영된다`(testDB: TestDB) {
            withUserTable(testDB) {
                //  
                val user1 = userRepository.get(1L)
                user1.shouldNotBeNull()

                // 캐시에 다시 저장 -> DB에도 저장됨
                val updatedUser = user1.copy(email = "updated@example.com")
                userRepository.put(updatedUser)

                // MapWriter 가 비동기로 실행되므로, 대기합니다.
                await until {
                    transaction {
                        UserSchema.findUserById(1L)?.email == updatedUser.email
                    }
                }

                // User 가 Update 가 되면 `updatedAt` 컬럼은 현재 시각으로 설정됩니다.
                val updatedUserFromDB = userRepository.findFreshById(1L)!!
                updatedUserFromDB.updatedAt.shouldNotBeNull()
                updatedUserFromDB shouldBeEqualTo updatedUser.copy(updatedAt = updatedUserFromDB.updatedAt)
            }
        }
    }

    @Nested
    inner class ReadWriteThroughWithNotDeleteDB: AbstractRedissonTest() {

        /**
         * Read-Write Through 캐시를 사용합니다.
         * 캐시에 저장하면 DB에도 저장됩니다. 다만, 캐시에서 삭제해도 DB에서는 삭제되지 않습니다.
         *
         * @see [RedisCacheConfig.deleteFromDBOnInvalidate]
         */
        private val userRepository by lazy {
            UserCacheRepository(
                redissonClient,
                "exposed:read-write:users",
                RedisCacheConfig.READ_WRITE_THROUGH
            )
        }

        @BeforeEach
        fun setup() {
            // 다른 테스트에서 캐시에 사용한 값을 모두 삭제한다.
            userRepository.invalidateAll()
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `deleteFromDBOnInvalidate=false 인 경우 캐시에서 삭제해도 DB에서는 삭제되지 않는다`(testDB: TestDB) {
            withUserTable(testDB) {
                // 캐시에서 조회한 값
                val userFromCache = userRepository.get(1L)
                userFromCache.shouldNotBeNull()

                // 캐시에서 삭제한다.
                userRepository.invalidate(1L)

                // 캐시에서 삭제 시, MapWriter 가 비동기로 실행되므로, 대기해야 정확한 결과를 얻을 수 있습니다.
                Thread.sleep(100)

                // 캐시에서 삭제했지만, DB에는 여전히 존재한다.
                val userFromDB = userRepository.findFreshById(1L)
                userFromDB shouldBeEqualTo userFromCache
            }
        }
    }
}
