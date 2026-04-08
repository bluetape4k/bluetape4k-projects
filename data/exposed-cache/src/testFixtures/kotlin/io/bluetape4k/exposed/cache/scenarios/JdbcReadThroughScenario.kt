package io.bluetape4k.exposed.cache.scenarios

import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable

/**
 * [JdbcCacheTestScenario] 기반 Read-Through 캐시 시나리오입니다.
 *
 * 캐시에서 조회 시 캐시 미스가 발생하면 DB에서 읽어 캐시에 저장하는 패턴을 검증합니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @param E 엔티티 타입
 */
interface JdbcReadThroughScenario<ID: Any, E: Serializable>: JdbcCacheTestScenario<ID, E> {

    companion object: KLogging() {
        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get - ID로 조회 시 DB에서 읽어서 캐시에 저장 후 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()

            // DB에서 조회한 값
            val entityFromDB = repository.findByIdFromDb(id)
            entityFromDB.shouldNotBeNull()

            // 캐시에서 조회한 값 (Read-Through)
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()
            entityFromCache shouldBeEqualTo entityFromDB

            repository.containsKey(id).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `containsKey - 캐시에 해당 ID가 존재하는지 검사, 없으면 DB에서 로드한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 없다면, Read through로 DB에서 로드합니다. DB에도 없다면 false를 반환합니다.
            ids.all { repository.containsKey(it) }.shouldBeTrue()

            // 캐시, DB 모두에 존재하지 않는 ID
            repository.containsKey(getNonExistentId()).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidate - Read-Through에서 캐시 invalidate는 DB에 영향을 주지 않는다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()

            // 먼저 캐시에 로드
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()

            // 캐시에서 삭제 (Read-Through Only인 경우 DB에는 영향 없음)
            getExistingIds().forEach { repository.invalidate(it) }

            // 다시 조회하면 DB에서 로드
            val reloadedEntity = repository.get(id)
            if (cacheWriteMode == io.bluetape4k.exposed.cache.CacheWriteMode.READ_ONLY) {
                reloadedEntity.shouldNotBeNull()
                reloadedEntity shouldBeEqualTo entityFromCache
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get - 존재하지 않는 ID로 캐시 조회하면 null을 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val nonExistentId = getNonExistentId()

            val entityFromDB = repository.findByIdFromDb(nonExistentId)
            entityFromDB.shouldBeNull()

            val entityFromCache = repository.get(nonExistentId)
            entityFromCache.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll - 전체 엔티티를 가져옵니다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val entities = repository.findAll()
            entities.shouldNotBeEmpty()
            entities.size shouldBeEqualTo
                    repository.table
                        .selectAll()
                        .count()
                        .toInt()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAll - 여러 ID의 엔티티를 한 번에 조회한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds() + getNonExistentId()
            val entities = repository.getAll(ids)
            entities.shouldNotBeEmpty()

            // 존재하지 않는 ID는 결과에 포함되지 않아야 함
            entities.size shouldBeEqualTo getExistingIds().size
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAll - 빈 목록은 빈 결과를 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            repository.getAll(emptyList()).shouldBeEmpty()
        }
    }
}
