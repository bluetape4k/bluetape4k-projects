package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.collections.toVarargArray
import io.bluetape4k.coroutines.flow.extensions.asFlow
import io.bluetape4k.exposed.dao.HasIdentifier
import io.bluetape4k.exposed.redisson.repository.scenarios.CacheTestScenario.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

interface SuspendedReadThroughScenario<T: HasIdentifier<ID>, ID: Any>: SuspendedCacheTestScenario<T, ID> {

    companion object: KLoggingChannel() {
        const val DEFAULT_DELAY = 100L
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get(id) - ID로 조회 시 DB에서 읽어서 캐시에 저장 후 반환한다`(testDB: TestDB) = runSuspendIO {
        withSuspendedEntityTable(testDB) {
            val id = getExistingId()

            // DB에서 조회한 값
            val entityFromDB = repository.findFreshById(id)
            entityFromDB.shouldNotBeNull()

            // 캐시에서 조회한 값
            val entityFromCAche = repository.get(id)
            entityFromCAche.shouldNotBeNull()
            entityFromCAche shouldBeEqualTo entityFromDB

            repository.exists(id).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists(id) - 캐시에 해당 ID가 존재하는지 검사, 실제 없다면 DB에서 로드한다`(testDB: TestDB) = runSuspendIO {
        withSuspendedEntityTable(testDB) {
            val ids = getExistingIds()

            // 캐시에 없다면, Read through로 DB에서 로드합니다. DB에도 없다면 false를 반환합니다.
            ids.all { repository.exists(it) }.shouldBeTrue()

            // 캐시, DB 모두에 존재하지 않는 ID
            repository.exists(getNonExistentId()).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidte(id) - Read through에서 캐시 invalidate 는 DB에 영향을 주지 않는다`(testDB: TestDB) = runSuspendIO {
        withSuspendedEntityTable(testDB) {
            val id = getExistingId()
            log.debug { "existingId: $id" }

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
    fun `존재하지 않는 ID로 캐시 조회하면, null을 반환한다`(testDB: TestDB) = runSuspendIO {
        withSuspendedEntityTable(testDB) {
            // 임의의 존재하지 않는 ID 생성 방법은 구현 클래스에서 정의
            val nonExistentId = getNonExistentId()

            val entityFromDB = repository.findFreshById(nonExistentId)
            entityFromDB.shouldBeNull()

            val entityFromCache = repository.get(nonExistentId)
            entityFromCache.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll - 전체 엔티티를 가져옵니다`(testDB: TestDB) = runTest {
        withSuspendedEntityTable(testDB) {

            val entities = repository.findAll()
            entities.shouldNotBeEmpty()
            entities.size shouldBeEqualTo repository.entityTable.selectAll().count().toInt()

            // @ParameterizedTest 때문에 testDB 들이 꼬인다... 대기 시간을 둬서, 다른 DB와의 영항을 미치지 않게 한다
            delay(DEFAULT_DELAY)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAll - 여러 ID의 엔티티를 한번에 조회한다`(testDB: TestDB) = runSuspendIO {
        withSuspendedEntityTable(testDB) {
            val ids = getExistingIds() + getNonExistentId()
            val entities = repository.getAll(ids, batchSize = 2)
            entities.shouldNotBeEmpty()

            entities.size shouldBeEqualTo getExistingIds().size
        }
    }

    /**
     * 단 설정한 코덱이 Map Key 에 대해서는 StringCodec 을 사용해야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `캐시 키 패턴으로 캐시 무효화하기`(testDB: TestDB) = runSuspendIO {
        withSuspendedEntityTable(testDB) {
            // @ParameterizedTest 때문에 testDB 들이 꼬인다... 대기 시간을 둬서, 다른 DB와의 영항을 미치지 않게 한다
            if (cacheConfig.isReadWrite) {
                delay(DEFAULT_DELAY)
            }

            repository.getAll(getExistingIds())

            val invalidated = repository.invalidateByPattern("*1*") +
                    ('A'..'Z').asFlow().map { repository.invalidateByPattern("*$it*") }.toList().sum()

            invalidated shouldBeGreaterThan 0
        }
    }
}
