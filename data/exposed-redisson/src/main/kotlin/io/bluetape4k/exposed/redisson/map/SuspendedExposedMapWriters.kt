package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.dao.HasIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.plus
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.v1.jdbc.update
import org.redisson.api.map.MapWriterAsync
import org.redisson.api.map.WriteMode
import java.util.concurrent.CompletionStage

private val defaultMapWriterCoroutineScope = CoroutineScope(Dispatchers.IO) + CoroutineName("DB-Writer")

/**
 * Redisson의 Write-through [MapWriterAsync] 를 Exposed와 코루틴를 사용하여 구현한 최상위 클래스입니다.
 *
 * @param writeToDb DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDb DB에서 데이터를 삭제하는 함수입니다.
 */
open class SuspendedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val writeToDb: suspend (map: Map<ID, E>) -> Unit,
    private val deleteFromDb: suspend (keys: Collection<ID>) -> Unit,
    private val scope: CoroutineScope = defaultMapWriterCoroutineScope,
): MapWriterAsync<ID, E> {

    companion object: KLoggingChannel() {
        private const val DEFAULT_QUERY_TIMEOUT = 30_000  // 30 seconds
    }

    override fun write(map: Map<ID, E>): CompletionStage<Void> = scope.async {
        suspendedTransactionAsync(context = scope.coroutineContext) {
            try {
                writeToDb(map)
            } catch (e: Throwable) {
                log.error(e) { "DB에 Write 중 오류 발생" }
                throw e
            }
        }.await()
        null
    }.asCompletableFuture()

    override fun delete(ids: Collection<ID>): CompletionStage<Void> = scope.async {
        suspendedTransactionAsync(context = scope.coroutineContext) {
            try {
                log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=$ids" }
                deleteFromDb(ids)
            } catch (e: Throwable) {
                log.error(e) { "DB에서 삭제 중 오류 발생" }
                throw e
            }
        }.await()
        null
    }.asCompletableFuture()
}

/**
 * `id`를 가진 엔티티를 DB에 Write 하기 위한 [SuspendedEntityMapWriter] 기본 구현체입니다.
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
    writeMode: WriteMode = WriteMode.WRITE_THROUGH,
): SuspendedEntityMapWriter<ID, E>(
    scope = scope,
    writeToDb = { map ->
        when (writeMode) {
            WriteMode.WRITE_THROUGH -> {
                writeThrough(map, entityTable, updateBody, batchInsertBody)
            }
            WriteMode.WRITE_BEHIND -> {
                writeBehind(map, entityTable, batchInsertBody)
            }
        }
    },
    deleteFromDb = { ids ->
        if (deleteFromDBOnInvalidate) {
            log.debug { "캐시가 Invalidated 되어, DB에서도 삭제합니다... ids=$ids, id type=${ids.firstOrNull()?.javaClass?.simpleName}" }

            // Map Key가 String Codec 인데, UUID로 변환을 못함
            @Suppress("UNCHECKED_CAST")
            val idsToDelete = ids.mapToLanguageType(entityTable.id) as List<ID>
            entityTable.deleteWhere { entityTable.id inList idsToDelete }
        }
    },
) {
    companion object: KLoggingChannel() {
        private const val DEFAULT_BATCH_SIZE = 1000

        private fun <K: Any, V: HasIdentifier<K>> writeThrough(
            map: Map<K, V>,
            entityTable: IdTable<K>,
            updateBody: IdTable<K>.(UpdateStatement, V) -> Unit,
            batchInsertBody: BatchInsertStatement.(V) -> Unit,
        ) {
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

            // Write Through 시에는 id가 DB에서 자동 생성되지 않는 경우에만 batchInsert 를 수행합니다.
            // Write Behind 시에는 항상 batchInsert 를 수행합니다.
            val canBatchInsert = entityTable.id.autoIncColumnType == null && !entityTable.id.isDatabaseGenerated()
            if (canBatchInsert) {
                val entitiesToInsert = map.values.filterNot { it.id in existIds }
                log.debug { "ID가 자동증가 타입이 아니므로, batchInsert 를 수행합니다...entities size=${entitiesToInsert.size}" }
                entityTable.batchInsert(entitiesToInsert) {
                    batchInsertBody(this, it)
                }
            }
        }

        private fun <K: Any, V: HasIdentifier<K>> writeBehind(
            map: Map<K, V>,
            entityTable: IdTable<K>,
            batchInsertBody: BatchInsertStatement.(V) -> Unit,
            batchSize: Int = DEFAULT_BATCH_SIZE,
        ) {
            // Write Behind 시에는 캐시에 남길 일이 없으므로, 자동증가 ID인 경우에도 batchInsert 를 수행합니다.
            val entitiesToInsert = map.values
            entitiesToInsert.chunked(batchSize).forEach { chunk ->
                log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=${chunk.map { it.id }}" }
                entityTable.batchInsert(chunk, shouldReturnGeneratedValues = false) {
                    batchInsertBody(this, it)
                }
            }
        }
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
