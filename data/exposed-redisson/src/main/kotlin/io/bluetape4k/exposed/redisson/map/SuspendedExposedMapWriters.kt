package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.plus
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.redisson.api.map.MapWriterAsync
import java.util.concurrent.CompletionStage

private val defaultMapWriterCoroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("DB-Writer")

open class SuspendedExposedMapWriter<ID: Any, E: Any>(
    private val writeToDb: suspend (map: Map<ID, E>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapWriterCoroutineScope,
): MapWriterAsync<ID, E> {

    override fun write(map: Map<ID, E>): CompletionStage<Void> = scope.async {
        newSuspendedTransaction(scope.coroutineContext) {
            writeToDb(map)
        }
        null
    }.asCompletableFuture()

    override fun delete(keys: Collection<ID>): CompletionStage<Void> = scope.async {
        newSuspendedTransaction(scope.coroutineContext) {
            deleteFromDb(keys)
        }
        null
    }.asCompletableFuture()
}

open class SuspendedExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    scope: CoroutineScope = defaultMapWriterCoroutineScope,
    private val updateBody: IdTable<ID>.(UpdateStatement, E) -> Unit,
    private val batchInsertBody: BatchInsertStatement.(E) -> Unit,
    private val deleteFromDBOnInvalidate: Boolean = true,
): SuspendedExposedMapWriter<ID, E>(
    scope = scope,
    writeToDb = { map ->
        val entityIdsToUpdate =
            entityTable.select(entityTable.id)
                .where { entityTable.id inList map.keys }
                .map { it[entityTable.id].value }

        val entitiesToUpdate = map.values.filter { it.id in entityIdsToUpdate }

        entitiesToUpdate.forEach { entity ->
            entityTable.update({ entityTable.id eq entity.id }) {
                updateBody(it, entity)
            }
        }

        val entitiesToInsert = map.values.filterNot { it.id in entityIdsToUpdate }

        entityTable.batchInsert(entitiesToInsert) { entity ->
            batchInsertBody(entity)
        }
    },
    deleteFromDb = { keys ->
        if (deleteFromDBOnInvalidate) {
            entityTable.deleteWhere { entityTable.id inList keys }
        }
    },
)
