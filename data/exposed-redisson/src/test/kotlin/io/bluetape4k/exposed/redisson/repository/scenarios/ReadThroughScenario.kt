package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.redisson.repository.scenarios.CacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

interface ReadThroughScenario<T: HasIdentifier<ID>, ID: Any>: CacheTestScenario<T, ID> {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get(id) - ID로 조회 시 DB에서 읽어서 캐시에 저장 후 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()

            // DB에서 조회한 값
            val entityFromDB = repository.findByIdFromDb(id)
            entityFromDB.shouldNotBeNull()

            // 캐시에서 조회한 값
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()
            entityFromCache shouldBeEqualTo entityFromDB

            repository.exists(id).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists(id) - 캐시에 해당 ID가 존재하는지 검사, 실제 없다면 DB에서 로드한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 없다면, Read through로 DB에서 로드합니다. DB에도 없다면 false를 반환합니다.
            ids.all { repository.exists(it) }.shouldBeTrue()

            // 캐시, DB 모두에 존재하지 않는 ID
            repository.exists(getNonExistentId()).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidte(id) - Read through에서 캐시 invalidate 는 DB에 영향을 주지 않는다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()

            // 먼저 캐시에 로드
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()

            // 캐시에서 삭제 (Read Through Only 인 경우에는 DB에는 영향을 주지 않음)
            repository.invalidate(*getExistingIds().toVarargArray())

            // 다시 조회하면 DB에서 로드
            val reloadedEntity = repository.get(id)
            if (cacheConfig.isReadOnly) {
                reloadedEntity.shouldNotBeNull()
                reloadedEntity shouldBeEqualTo entityFromCache
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `존재하지 않는 ID로 캐시 조회하면, null을 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            // 임의의 존재하지 않는 ID 생성 방법은 구현 클래스에서 정의
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
            entities.size shouldBeEqualTo repository.entityTable.selectAll().count().toInt()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAllBatch - 여러 ID의 엔티티를 한번에 조회한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds() + getNonExistentId()
            val entities = repository.getAll(ids, batchSize = 2)
            entities.shouldNotBeEmpty()

            entities.size shouldBeEqualTo ids.size - 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAllBatch - batchSize 는 0보다 커야 한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            assertFailsWith<IllegalArgumentException> {
                repository.getAll(getExistingIds(), batchSize = 0)
            }
        }
    }

    /**
     * 단 설정한 코덱이 Map Key 에 대해서는 StringCodec 을 사용해야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `캐시 키 패턴으로 캐시 무효화하기`(testDB: TestDB) {
        withEntityTable(testDB) {
            repository.getAll(getExistingIds())

            val invalidated = repository.invalidateByPattern("*1*") +
                    ('A'..'Z').sumOf { repository.invalidateByPattern("*$it*") }

            invalidated shouldBeGreaterThan 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidateByPattern - count 는 0보다 커야 한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            assertFailsWith<IllegalArgumentException> {
                repository.invalidateByPattern("*", count = 0)
            }
        }
    }
}
