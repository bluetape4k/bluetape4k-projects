package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.redisson.api.map.MapWriterAsync
import java.util.concurrent.CompletionStage

open class SuspendedExposedMapWriter<ID: Any, E: Any>(
    private val writeToDb: suspend (map: Map<ID, E>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<ID>) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
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

open class DefaultSuspendedExposedMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    private val toEntity: ResultRow.() -> E,
    private val updateBody: IdTable<ID>.(UpdateStatement, E) -> Unit,
    private val batchInsertBody: BatchInsertStatement.(E) -> Unit,
    private val deleteFromDbOnInvalidate: Boolean = true,
): SuspendedExposedMapWriter<ID, E>(
    writeToDb = { map ->
        val entityIdsToUpdate =
            entityTable.select(entityTable.id)
                .where { entityTable.id inList map.keys }
                .map { it[entityTable.id].value }

        val entityToUpdate = map.values.filter { it.id in entityIdsToUpdate }

        entityTable.batchInsert(entityToUpdate) { entity ->
            batchInsertBody(entity)
        }

        if (deleteFromDbOnInvalidate) {
            entityTable.deleteWhere { entityTable.id notInList map.keys }
        }
    },
    deleteFromDb = { keys ->
        if (deleteFromDbOnInvalidate) {
            entityTable.deleteWhere { entityTable.id inList keys }
        }
    },
)
