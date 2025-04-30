package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
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

    companion object: KLogging() {
        private const val DEFAULT_QUERY_TIMEOUT = 30_000  // 30 seconds
    }

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
    deleteFromDBOnInvalidate: Boolean = false,
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
    deleteFromDb = { ids ->
        if (deleteFromDBOnInvalidate) {
            log.debug { "캐시가 Invalidated 되어, DB에서도 삭제합니다 (deleteFromDBOnInvalidate=$deleteFromDBOnInvalidate)... ids=$ids" }
            entityTable.deleteWhere { entityTable.id inList ids }
        }
    },
) {
    companion object: KLogging() {
        private const val DEFAULT_BATCH_SIZE = 1000
    }

    // 필드로 따로 저장하여 로깅 용도로 사용
    private val deleteFromDBOnInvalidate: Boolean

    init {
        // 중요 설정 변경 시 경고 로그
        if (deleteFromDBOnInvalidate) {
            log.warn {
                "⚠️ 주의! deleteFromDBOnInvalidate=true로 설정되었습니다. " +
                        "캐시에서 항목 삭제 시 DB에서도 함께 삭제됩니다. 프로덕션 환경에서는 신중히 사용하세요."
            }
        }
        this.deleteFromDBOnInvalidate = deleteFromDBOnInvalidate
    }
}
