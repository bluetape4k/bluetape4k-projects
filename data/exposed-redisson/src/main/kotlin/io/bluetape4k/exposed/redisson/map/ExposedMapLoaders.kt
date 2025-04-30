package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.redisson.api.map.MapLoader

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
