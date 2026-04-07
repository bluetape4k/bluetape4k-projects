package io.bluetape4k.exposed.cache.scenarios

import java.io.Serializable

import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [JdbcCacheTestScenario] 기반 Write-Through 캐시 시나리오입니다.
 *
 * 캐시에 저장하면 DB에도 동시 반영되는 패턴을 검증합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
interface JdbcWriteThroughScenario<ID: Any, E: Serializable>: JdbcCacheTestScenario<ID, E> {

    companion object: KLogging() {
        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"
    }

    /**
     * 새로운 엔티티를 생성합니다.
     *
     * @return 새로 생성된 엔티티
     */
    fun createNewEntity(): E

    /**
     * 엔티티의 이메일(또는 변경 대상 필드)을 수정합니다.
     *
     * @param entity 수정 대상 엔티티
     * @return 수정된 엔티티
     */
    fun updateEntityEmail(entity: E): E

    /**
     * 감사(audit) 필드를 제외하고 두 엔티티가 동일한지 검증합니다.
     *
     * @param entity1 비교 대상 엔티티 1
     * @param entity2 비교 대상 엔티티 2
     */
    fun assertSameEntityWithoutUpdatedAt(
        entity1: E,
        entity2: E,
    )

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `put - 캐시에 저장하면 DB에도 저장된다`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val id = getExistingId()

            // 캐시에서 조회한 값
            val entity = repository.get(id)
            entity.shouldNotBeNull()

            // 캐시에 갱신된 값 저장 -> DB에도 저장
            val updatedEntity = updateEntityEmail(entity)
            repository.put(repository.extractId(updatedEntity), updatedEntity)

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
    fun `putAll - 캐시에 저장하면 DB에도 저장된다`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val ids = getExistingIds()

            // 캐시에서 조회한 값
            val entities = repository.getAll(ids)
            entities.shouldNotBeEmpty()
            entities.shouldHaveSize(ids.size)

            // 캐시에 갱신된 값 저장 -> DB에도 저장
            val updatedEntities = entities.values.map { updateEntityEmail(it) }
            val updatedMap = updatedEntities.associateBy { repository.extractId(it) }
            repository.putAll(updatedMap)

            // 캐시에서 조회한 값
            val entitiesFromCache = repository.getAll(ids)
            entitiesFromCache.shouldNotBeNull()
            entitiesFromCache.values.forEach { entity ->
                assertSameEntityWithoutUpdatedAt(
                    entity,
                    updatedEntities.find {
                        repository.extractId(it) == repository.extractId(entity)
                    }!!
                )
            }

            // DB에서 조회한 값
            val entitiesFromDB = repository.findAllFromDb(ids)
            entitiesFromDB.shouldNotBeEmpty()
            entitiesFromDB shouldHaveSize ids.size

            entitiesFromDB.forEach { entity ->
                assertSameEntityWithoutUpdatedAt(
                    entity,
                    entitiesFromCache.values.find {
                        repository.extractId(it) == repository.extractId(entity)
                    }!!
                )
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `putAll - 새 엔티티 추가 시 AutoInc Id는 DB 저장 안 하고 Client 생성 Id는 DB에 저장된다`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val prevCount = repository.table.selectAll().count()
            val newEntities = List(5) { createNewEntity() }
            val newMap = newEntities.associateBy { repository.extractId(it) }
            repository.putAll(newMap)

            val newCount = repository.table.selectAll().count()

            // id가 DB에서 자동증가하지 않는 경우에만 batchInsert를 수행합니다.
            if (repository.table.id.autoIncColumnType == null) {
                newCount shouldBeEqualTo prevCount + newEntities.size
            } else {
                newCount shouldBeEqualTo prevCount
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidate - 캐시 invalidate 시 DB에 영향을 줄 수 있다`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withEntityTable(testDB) {
            val id = getExistingId()

            // 먼저 캐시에 로드
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()

            // 캐시에서 삭제
            repository.invalidate(id)

            // 캐시에서 삭제했지만, DB에는 여전히 존재한다.
            val entityFromDB = repository.findByIdFromDb(id)
            entityFromDB shouldBeEqualTo entityFromCache
        }
    }
}
