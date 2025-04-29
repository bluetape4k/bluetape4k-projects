package io.bluetape4k.exposed.redisson

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.redisson.api.map.MapLoader

/**
 * Redisson의 Read-thgrough MapLoader 를 구현한 것입니다.
 *
 * @param loadFromDb DB에서 key에 해당하는 value를 가져오는 함수입니다.
 * @param loadAllKeysFromDb DB에서 모든 key를 가져오는 함수입니다.
 */
@Deprecated("Use `DefaultExposedMapLoader` instead.")
open class ExposedMapLoader<K: Any, V: Any>(
    private val loadFromDb: (K) -> V?,
    private val loadAllKeysFromDb: () -> Collection<K>,
): MapLoader<K, V> {

    companion object: KLogging()

    override fun load(key: K): V? = transaction {
        log.debug { "Load from DB... key=$key" }
        loadFromDb(key)
            .apply {
                log.debug { "Loaded from DB... key=$key, entity=$this" }
            }
    }

    override fun loadAllKeys(): Iterable<K>? = transaction {
        log.debug { "Load all keys from DB..." }
        loadAllKeysFromDb()
    }
}

/**
 * Entity<K> 를 위한 [ExposedMapLoader] 기본 구현체입니다.
 *
 * @param table Entity<ID> 를 위한 [IdTable] 입니다.
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 */
@Deprecated("Use `DefaultExposedMapLoader` instead")
open class ExposedEntityMapLoader<ID: Any, E: HasIdentifier<ID>>(
    private val table: IdTable<ID>,
    private val toEntity: ResultRow.() -> E,
): ExposedMapLoader<ID, E>(
    loadFromDb = { id: ID ->
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

    },
    loadAllKeysFromDb = {
        table.selectAll().map { it[table.id].value }
    }
) 
