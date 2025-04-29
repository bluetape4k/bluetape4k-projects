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

    companion object: KLogging()

    override fun load(id: ID): E? = transaction {
        log.debug { "DB에서 엔티티 로드... id=$id" }
        loadByIdFromDB(id)
            .apply {
                log.debug { "DB에서 엔티티 로드. id=$id, entity=$this" }
            }
    }

    override fun loadAllKeys(): Iterable<ID>? = transaction {
        queryTimeout = 30_000  // 30 seconds
        log.debug { "DB에서 모든 id 를 로드합니다..." }
        loadAllIdsFromDB()
    }
}

open class ExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val table: IdTable<ID>,
    private val batchSize: Int = 1000,
    private val toEntity: ResultRow.() -> E,
): ExposedMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()
    },
    loadAllIdsFromDB = {
        val recordCount = table.selectAll().count()
        var offset = 0L
        val limit = batchSize

        generateSequence<List<ID>> {
            val rows = table.selectAll()
                .limit(limit)
                .offset(offset)
                .map { it[table.id].value }
            offset += limit
            rows
        }
            .takeWhile { offset < recordCount }
            .asIterable()
            .flatMap { it }
    }
) {
    companion object: KLogging()
}
