package io.bluetape4k.exposed.redisson

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.redisson.api.map.MapWriter

/**
 *  Redisson의 Write-through MapWriter 를 구현한 것입니다.
 *
 * @param writeToDb DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDb DB에서 데이터를 삭제하는 함수입니다.
 */
open class ExposedMapWriter<K: Any, V: Any>(
    private val writeToDb: (map: Map<K, V>) -> Unit,
    private val deleteFromDb: (keys: Collection<K>) -> Unit,
): MapWriter<K, V> {

    companion object: KLogging()

    override fun write(map: Map<K, V>) = transaction {
        writeToDb(map)
    }

    override fun delete(keys: Collection<K>) = transaction {
        deleteFromDb(keys)
    }
}

/**
 * Entity<K> 를 위한 [ExposedMapWriter] 기본 구현체입니다.
 *
 * @param entityTable Entity<ID> 를 위한 [IdTable] 입니다.
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 * @param updateBody DB에 이미 존재하는 ID인 경우 UPDATE 하도록 하는 쿼리 입니다.
 * @param batchInsertBody 새로운 엔티티라면 batchInsert 를 수행하도록 하는 쿼리 입니다.
 * @param deleteFromDbOnInvalidate 캐시에서 삭제될 때, DB에서도 삭제할 것인지 여부를 나타냅니다.
 */
open class ExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    private val toEntity: ResultRow.() -> E,
    private val updateBody: IdTable<ID>.(UpdateStatement, E) -> Unit,
    private val batchInsertBody: BatchInsertStatement.(E) -> Unit,
    private val deleteFromDbOnInvalidate: Boolean = true,
): ExposedMapWriter<ID, E>(
    writeToDb = { map ->
        val entityIdsToUpdate =
            entityTable.select(entityTable.id)
                .where { entityTable.id inList map.keys }
                .map { it[entityTable.id].value }

        val entityToUpdate = map.values.filter { it.id in entityIdsToUpdate }
        entityToUpdate.forEach { entity ->
            entityTable.update({ entityTable.id eq entity.id }) {
                updateBody(it, entity)
            }
        }

        val entityToInsert = map.values.filter { it.id !in entityIdsToUpdate }
        entityTable.batchInsert(
            entityToInsert,
            shouldReturnGeneratedValues = false,
        ) { entity ->
            batchInsertBody(entity)
        }
    },
    deleteFromDb = { keys ->
        if (deleteFromDbOnInvalidate) {
            entityTable.deleteWhere { entityTable.id inList keys }
        }
    }
)
