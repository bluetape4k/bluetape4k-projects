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
        log.debug { "Load from DB... id=$id" }
        loadByIdFromDB(id)
            .apply {
                log.debug { "Loaded from DB... id=$id, E=$this" }
            }
    }

    override fun loadAllKeys(): Iterable<ID>? = transaction {
        log.debug { "Load all ids from DB..." }
        loadAllIdsFromDB()
    }
}

open class DefaultExposedMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val table: IdTable<ID>,
    private val toEntity: ResultRow.() -> E,
): ExposedMapLoader<ID, E>(
    loadByIdFromDB = { id: ID ->
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()
    },
    loadAllIdsFromDB = {
        table.selectAll().map { it[table.id].value }
    }
) {
    companion object: KLogging()
}
