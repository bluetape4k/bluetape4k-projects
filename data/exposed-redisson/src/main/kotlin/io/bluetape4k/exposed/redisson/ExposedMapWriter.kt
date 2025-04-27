package io.bluetape4k.exposed.redisson

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.redisson.api.map.MapWriter

/**
 *  Redisson의 Write-through MapWriter 를 구현한 것입니다.
 *
 * @param writeToDb DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDb DB에서 데이터를 삭제하는 함수입니다.
 */
open class ExposedMapWriter<K: Any, V: Any>(
    private val writeToDb: (map: Map<K, V?>) -> Unit,
    private val deleteFromDb: (keys: Collection<K>) -> Unit,
): MapWriter<K, V> {

    companion object: KLogging()

    override fun write(map: Map<K, V?>) = transaction {
        writeToDb(map)
    }

    override fun delete(keys: Collection<K>) = transaction {
        deleteFromDb(keys)
    }
}

/**
 * Entity<K> 를 위한 [ExposedMapWriter] 기본 구현체입니다.
 *
 * @param table Entity<ID> 를 위한 [IdTable] 입니다.
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 */
open class ExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val table: IdTable<ID>,
    private val writeToDB: (map: Map<ID, E?>) -> Unit,
): ExposedMapWriter<ID, E>(
    writeToDb = writeToDB,
    deleteFromDb = { ids ->
        table.deleteWhere { table.id inList ids }
    }
)
