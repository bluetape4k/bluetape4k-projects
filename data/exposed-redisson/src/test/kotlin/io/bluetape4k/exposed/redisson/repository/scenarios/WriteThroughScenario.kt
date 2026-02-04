package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.redisson.repository.scenarios.CacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

interface WriteThroughScenario<T: HasIdentifier<ID>, ID: Any>: CacheTestScenario<T, ID> {

    companion object: KLogging()

    fun createNewEntity(): T

    fun updateEntityEmail(entity: T): T

    fun assertSameEntityWithoutUpdatedAt(entity1: T, entity2: T)

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - 캐시에 저장하면, DB에도 저장된다`(testDB: TestDB) {
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
            val entityFromDB = repository.findByIdFromDb(id)
            entityFromDB.shouldNotBeNull()

            assertSameEntityWithoutUpdatedAt(entityFromDB, entityFromCache)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - 캐시에 저장하면, DB에도 저장된다`(testDB: TestDB) {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val ids = getExistingIds()

            // 캐시에서 조회한 값
            val entities = repository.getAll(ids)
            entities.shouldNotBeEmpty()
            entities shouldHaveSize ids.size

            // 캐시에 갱신된 값 저장 -> DB에도 저장
            val updatedEntities = entities.map { updateEntityEmail(it) }
            repository.putAll(updatedEntities)

            // 캐시에서 조회한 값
            val entitiesFromCache = repository.getAll(ids)
            entitiesFromCache.shouldNotBeNull()
            entitiesFromCache.forEach { entity ->
                assertSameEntityWithoutUpdatedAt(entity, updatedEntities.find { it.id == entity.id }!!)
            }

            // DB에서 조회한 값
            val entitiesFromDB = repository.findAllFromDb(ids)
            entitiesFromDB.shouldNotBeEmpty() shouldHaveSize ids.size

            entitiesFromDB.forEach { entity ->
                assertSameEntityWithoutUpdatedAt(entity, entitiesFromCache.find { it.id == entity.id }!!)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - 새로운 Record를 추가하면 AutoInc Id 는 DB 저장을 하지 않고, Client 생성 Id는 DB에 저장된다`(testDB: TestDB) {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val prevCount = repository.entityTable.selectAll().count()
            val newEntities = fastList(5) { createNewEntity() }
            repository.putAll(newEntities)

            val newCount = repository.entityTable.selectAll().count()

            // id가 DB에서 자동증가하지 않는 경우에만 batchInsert 를 수행합니다.
            if (repository.entityTable.id.autoIncColumnType == null) {
                newCount shouldBeEqualTo prevCount + newEntities.size
            } else {
                newCount shouldBeEqualTo prevCount
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidte(id) - 캐시 invalidate 시 DB에 영향을 줄 수 있다`(testDB: TestDB) {
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
                val userFromDB = repository.findByIdFromDb(id)
                userFromDB.shouldBeNull()
            } else {
                // 캐시에서 삭제했지만, DB에는 여전히 존재한다.
                val userFromDB = repository.findByIdFromDb(id)
                userFromDB shouldBeEqualTo entityFromCache
            }
        }
    }
}
