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
import org.jetbrains.exposed.sql.autoIncColumnType
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.vendors.PostgreSQLDialect
import org.redisson.api.map.MapWriterAsync
import java.util.concurrent.CompletionStage

private val defaultMapWriterCoroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("DB-Writer")

/**
 * Redisson의 Write-through [MapWriterAsync] 를 Exposed와 코루틴를 사용하여 구현한 최상위 클래스입니다.
 *
 * @param writeToDb DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDb DB에서 데이터를 삭제하는 함수입니다.
 */
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

/**
 * `id`를 가진 엔티티를 DB에 Write 하기 위한 [SuspendedExposedMapWriter] 기본 구현체입니다.
 *
 * @param entityTable Entity<ID> 를 위한 [IdTable] 입니다.
 * @param updateBody DB에 이미 존재하는 ID인 경우 UPDATE 하도록 하는 쿼리 입니다.
 * @param batchInsertBody 새로운 엔티티라면 batchInsert 를 수행하도록 하는 쿼리 입니다.
 * @param deleteFromDBOnInvalidate 캐시에서 삭제될 때, DB에서도 삭제할 것인지 여부를 나타냅니다.
 */
open class SuspendedExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    scope: CoroutineScope = defaultMapWriterCoroutineScope,
    private val updateBody: IdTable<ID>.(UpdateStatement, E) -> Unit,
    private val batchInsertBody: BatchInsertStatement.(E) -> Unit,
    deleteFromDBOnInvalidate: Boolean = false,
): SuspendedExposedMapWriter<ID, E>(
    scope = scope,
    writeToDb = { map ->
        log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=${map.keys}" }
        val existIds =
            entityTable.select(entityTable.id)
                .where { entityTable.id inList map.keys }
                .map { it[entityTable.id].value }

        val entitiesToUpdate = map.values.filter { it.id in existIds }

        entitiesToUpdate.forEach { entity ->
            entityTable.update({ entityTable.id eq entity.id }) {
                updateBody(it, entity)
            }
        }

        val entitiesToInsert = map.values.filterNot { it.id in existIds }
        // id가 DB에서 자동증가하지 않는 경우에만 batchInsert 를 수행합니다.
        if (entityTable.id.autoIncColumnType == null) {
            val entitiesToInsert = map.values.filterNot { it.id in existIds }
            log.debug { "ID가 자동증가 타입이 아니므로, batchInsert 를 수행합니다...entities=${entitiesToInsert}" }
            entityTable.batchInsert(entitiesToInsert) {
                batchInsertBody(this, it)
            }
        }
    },
    deleteFromDb = { ids ->
        if (deleteFromDBOnInvalidate) {
            log.debug { "캐시가 Invalidated 되어, DB에서도 삭제합니다... ids=$ids, id type=${ids.firstOrNull()?.javaClass?.simpleName}" }

            // PostgreSQLDialect 에서 id 타입을 맞추기 위해 변환합니다.
            @Suppress("UNCHECKED_CAST")
            val idsToDelete = if (TransactionManager.current().db.dialect is PostgreSQLDialect) {
                ids.mapToLanguageType(entityTable.id) as List<ID>
            } else {
                ids
            }

            entityTable.deleteWhere { entityTable.id inList idsToDelete }
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
