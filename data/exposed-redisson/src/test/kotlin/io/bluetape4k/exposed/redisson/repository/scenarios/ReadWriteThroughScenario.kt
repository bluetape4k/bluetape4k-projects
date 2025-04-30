package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

abstract class ReadWriteThroughScenario<T: HasIdentifier<ID>, ID: Any>: ReadThroughScenario<T, ID>() {

    companion object: KLogging()

    abstract fun updateEntityEmail(entity: T): T

    abstract fun assertSameEntityWithoutUpdatedAt(entity1: T, entity2: T)

    abstract val cacheConfig: RedisCacheConfig

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    open fun `put - 캐시에 저장하면, DB에도 저장된다`(testDB: TestDB) {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val id = getExistingId()

            // 캐시에서 조회한 값
            val entity = repository.get(id)
            entity.shouldNotBeNull()

            // 캐시에 갱신된 값 저장 -> DB에도 저장
            val updatedEntity = updateEntityEmail(entity)
            repository.put(updatedEntity)

            // 캐시에서 조회한 값
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()
            assertSameEntityWithoutUpdatedAt(entityFromCache, updatedEntity)

            // DB에서 조회한 값
            val entityFromDB = repository.findFreshById(id)
            entityFromDB.shouldNotBeNull()

            assertSameEntityWithoutUpdatedAt(entityFromCache, entityFromDB)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    override fun `invalidte(id) - 캐시 invalidate`(testDB: TestDB) {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val id = getExistingId()

            // 먼저 캐시에 로드
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()

            // 캐시에서 삭제
            repository.invalidate(id)

            if (cacheConfig.deleteFromDBOnInvalidate) {
                // 캐시에서 삭제했으므로, DB에서도 삭제된다.
                val userFromDB = repository.findFreshById(id)
                userFromDB.shouldBeNull()
            } else {
                // 캐시에서 삭제했지만, DB에는 여전히 존재한다.
                val userFromDB = repository.findFreshById(id)
                userFromDB shouldBeEqualTo entityFromCache
            }
        }
    }
}
