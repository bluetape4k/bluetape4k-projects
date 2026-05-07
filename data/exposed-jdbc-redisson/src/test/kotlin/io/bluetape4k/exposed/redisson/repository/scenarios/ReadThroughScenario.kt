package io.bluetape4k.exposed.redisson.repository.scenarios

import io.bluetape4k.exposed.redisson.AbstractRedissonTest.Companion.ENABLE_DIALECTS_METHOD
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import io.bluetape4k.assertions.assertFailsWith

interface ReadThroughScenario<ID: Any, E: java.io.Serializable>: CacheTestScenario<ID, E> {
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

            repository.containsKey(id).shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists(id) - 캐시에 해당 ID가 존재하는지 검사, 실제 없다면 DB에서 로드한다`(testDB: TestDB) {
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
    fun `invalidte(id) - Read through에서 캐시 invalidate 는 DB에 영향을 주지 않는다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val id = getExistingId()

            // 먼저 캐시에 로드
            val entityFromCache = repository.get(id)
            entityFromCache.shouldNotBeNull()

            // 캐시에서 삭제 (Read Through Only 인 경우에는 DB에는 영향을 주지 않음)
            repository.invalidateAll(getExistingIds())

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
            entities.size shouldBeEqualTo
                    repository.table
                        .selectAll()
                        .count()
                        .toInt()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAllBatch - 여러 ID의 엔티티를 한번에 조회한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val ids = getExistingIds() + getNonExistentId()
            val entities = repository.getAll(ids)
            entities.shouldNotBeEmpty()

            entities.size shouldBeEqualTo ids.size - 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAllBatch - 빈 목록은 빈 결과를 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            repository.getAll(emptyList()).shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getAllBatch - batchSize 는 0보다 커야 한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            assertFailsWith<IllegalArgumentException> {
                (repository as io.bluetape4k.exposed.redisson.repository.AbstractJdbcRedissonRepository<ID, E>)
                    .getAll(getExistingIds(), batchSize = 0)
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

            val invalidated =
                repository.invalidateByPattern("*1*") +
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

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `invalidateByPattern - 매칭되는 키가 없으면 0을 반환한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            repository.invalidateByPattern("not-exists-*") shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countFromDb - DB에서 전체 레코드 수를 직접 조회한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val count = repository.countFromDb()
            count shouldBeEqualTo repository.table.selectAll().count()
            count shouldBeGreaterThan 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll - limit, offset 파라미터로 페이지 조회한다`(testDB: TestDB) {
        withEntityTable(testDB) {
            val all = repository.findAll()
            all.shouldNotBeEmpty()

            val page = repository.findAll(limit = 1, offset = 0L)
            page.shouldNotBeEmpty()
            page.size shouldBeEqualTo 1

            val page2 = repository.findAll(limit = 1, offset = 1L)
            page2.shouldNotBeEmpty()
            page2.size shouldBeEqualTo 1
        }
    }
}
