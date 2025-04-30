package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.redisson.api.map.MapLoader

/**
 * ExposedMapLoader는 Exposed를 사용하여 DB에서 데이터를 로드하는 [MapLoader]입니다.
 *
 * @param ID ID 타입
 * @param E 엔티티 타입
 * @param loadByIdFromDB ID로 엔티티를 로드하는 함수
 * @param loadAllIdsFromDB 모든 ID를 로드하는 함수
 */
open class ExposedMapLoader<ID: Any, E: Any>(
    private val loadByIdFromDB: (ID) -> E?,
    private val loadAllIdsFromDB: () -> Collection<ID>,
): MapLoader<ID, E> {

    companion object: KLogging() {
        private const val DEFAULT_QUERY_TIMEOUT = 30_000  // 30 seconds
    }

    override fun load(id: ID): E? = transaction {
        log.debug { "DB에서 엔티티를 로드합니다... id=$id" }
        loadByIdFromDB(id)
            .apply {
                log.debug { "DB에서 엔티티를 로드했습니다. id=$id, entity=$this" }
            }
    }

    override fun loadAllKeys(): Iterable<ID>? = transaction {
        log.debug { "DB에서 모든 id 를 로드합니다..." }
        queryTimeout = DEFAULT_QUERY_TIMEOUT
        loadAllIdsFromDB()
    }
}

/**
 * [HasIdentifier]를 구현한 엔티티를 위한 [ExposedMapLoader]입니다.
 *
 * @sample io.bluetape4k.exposed.redisson.repository.AbstractExposedCacheRepository
 *
 * @param ID ID 타입
 * @param E 엔티티 타입
 * @param entityTable `EntityID<ID>` 를 id 컬럼으로 가진 [IdTable] 입니다.
 * @param batchSize 배치 사이즈
 * @param toEntity ResultRow를 엔티티로 변환하는 함수
 */
open class ExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val toEntity: ResultRow.() -> E,
): ExposedMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        entityTable.selectAll()
            .where { entityTable.id eq id }
            .singleOrNull()
            ?.toEntity()
    },
    loadAllIdsFromDB = {
        // 성능 상의 문제로 batch 방식으로 모든 ID를 로드합니다.
        val recordCount = entityTable.selectAll().count()
        var offset = 0L
        val limit = batchSize

        generateSequence<List<ID>> {
            entityTable.selectAll()
                .limit(limit)
                .offset(offset)
                .map { it[entityTable.id].value }
                .apply {
                    offset += limit
                }
        }.takeWhile { offset < recordCount }.asIterable().flatMap { it }
    }
) {
    companion object: KLogging() {
        private const val DEFAULT_BATCH_SIZE = 1000
    }
}
