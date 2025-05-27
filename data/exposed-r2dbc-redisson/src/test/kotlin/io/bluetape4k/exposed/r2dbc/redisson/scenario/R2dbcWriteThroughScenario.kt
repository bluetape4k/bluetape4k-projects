package io.bluetape4k.exposed.r2dbc.redisson.scenario

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.r2dbc.redisson.scenario.R2dbcCacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.awaitility.coUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration

interface R2dbcWriteThroughScenario<T: HasIdentifier<ID>, ID: Any>: R2dbcCacheTestScenario<T, ID> {

    companion object: KLoggingChannel() {
        const val DEFAULT_DELAY = 100L
    }

    suspend fun createNewEntity(): T

    suspend fun updateEntityEmail(entity: T): T

    suspend fun assertSameEntityWithoutAudit(entity1: T, entity2: T)

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - 캐시에 저장하면, DB에도 저장된다`(testDB: TestDB) = runSuspendIO {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withR2dbcEntityTable(testDB) {
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
            assertSameEntityWithoutAudit(entityFromCache, updatedEntity)

            delay(DEFAULT_DELAY)

            // DB에서 조회한 값
            val entityFromDB = repository.findFreshById(id)
            entityFromDB.shouldNotBeNull()

            assertSameEntityWithoutAudit(entityFromDB, entityFromCache)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - 캐시에 저장하면, DB에도 저장된다`(testDB: TestDB) = runSuspendIO {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withR2dbcEntityTable(testDB) {
            val ids = getExistingIds()
            delay(10)

            await.withPollInterval(Duration.ofMillis(100))
                .coUntil { getExistingIds().size == 3 }

            // @ParameterizedTest 때문에 testDB 들이 꼬인다... 대기 시간을 둬서, 다른 DB와의 영항을 미치지 않게 한다
            if (cacheConfig.isReadWrite) {
                delay(DEFAULT_DELAY)
            }

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
                assertSameEntityWithoutAudit(entity, updatedEntities.find { it.id == entity.id }!!)
            }

            // @ParameterizedTest 때문에 testDB 들이 꼬인다... 대기 시간을 둬서, 다른 DB와의 영항을 미치지 않게 한다
            if (cacheConfig.isReadWrite) {
                delay(DEFAULT_DELAY)
            }

            // DB에서 조회한 값
            val entitiesFromDB = repository.findFreshAll(*ids.toVarargArray()).toList()
            entitiesFromDB.shouldNotBeEmpty() shouldHaveSize ids.size

            entitiesFromDB.forEach { entity ->
                assertSameEntityWithoutAudit(entity, entitiesFromCache.find { it.id == entity.id }!!)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - 새로운 DTO를 추가하면 AutoInc Id 는 DB 저장을 하지 않고, Client 생성 Id는 DB에 저장된다`(testDB: TestDB) = runSuspendIO {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withR2dbcEntityTable(testDB) {
            val prevCount = repository.entityTable.selectAll().count()
            val newEntities = List(5) { createNewEntity() }
            repository.putAll(newEntities)

            // @ParameterizedTest 때문에 testDB 들이 꼬인다... 대기 시간을 둬서, 다른 DB와의 영항을 미치지 않게 한다
            if (cacheConfig.isReadWrite) {
                delay(DEFAULT_DELAY)
            }

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
    fun `invalidte(id) - 캐시 invalidate 시 DB에 영향을 줄 수 있다`(testDB: TestDB) = runSuspendIO {
        // NOTE: MySQL/MariaDB 에서는 Isolation level을 java.sql.Connection.TRANSACTION_READ_COMMITTED 로 설정해야 제대로 작동합니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withR2dbcEntityTable(testDB) {
            val id = getExistingId()

            // 먼저 캐시에 로드
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()

            // 캐시에서 삭제
            repository.invalidate(id)

            // @ParameterizedTest 때문에 testDB 들이 꼬인다... 대기 시간을 둬서, 다른 DB와의 영항을 미치지 않게 한다
            if (cacheConfig.isReadWrite) {
                delay(DEFAULT_DELAY)
            }

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
