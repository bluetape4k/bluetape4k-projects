package io.bluetape4k.exposed.r2dbc.redisson.map

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.core.mapToLanguageType
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import org.redisson.api.map.WriteMode

/**
 * `id`를 가진 엔티티를 DB에 Write 하기 위한 [R2dbcEntityMapWriter] 기본 구현체입니다.
 *
 * @param entityTable Entity<ID> 를 위한 [IdTable] 입니다.
 * @param updateBody DB에 이미 존재하는 ID인 경우 UPDATE 하도록 하는 쿼리 입니다.
 * @param batchInsertBody 새로운 엔티티라면 batchInsert 를 수행하도록 하는 쿼리 입니다.
 * @param deleteFromDBOnInvalidate 캐시에서 삭제될 때, DB에서도 삭제할 것인지 여부를 나타냅니다.
 */
open class R2dbcExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    scope: CoroutineScope = defaultMapWriterCoroutineScope,
    private val updateBody: IdTable<ID>.(UpdateStatement, E) -> Unit,
    private val batchInsertBody: BatchInsertStatement.(E) -> Unit,
    deleteFromDBOnInvalidate: Boolean = false,
    writeMode: WriteMode = WriteMode.WRITE_THROUGH,
): R2dbcEntityMapWriter<ID, E>(
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

        private suspend fun <K: Any, V: HasIdentifier<K>> writeThrough(
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
                    .toList()

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
                entitiesToInsert.chunked(DEFAULT_BATCH_SIZE).forEach { chunk ->
                    entityTable.batchInsert(chunk, shouldReturnGeneratedValues = false) {
                        batchInsertBody(this, it)
                    }
                }
            }
        }

        private suspend fun <K: Any, V: HasIdentifier<K>> writeBehind(
            map: Map<K, V>,
            entityTable: IdTable<K>,
            batchInsertBody: BatchInsertStatement.(V) -> Unit,
            batchSize: Int = DEFAULT_BATCH_SIZE,
        ) {
            // Write Behind 시에는 캐시에 남길 일이 없으므로, 자동증가 ID인 경우에도 batchInsert 를 수행합니다.
            val entitiesToInsert = map.values
            entitiesToInsert
                .chunked(batchSize)
                .asFlow()
                .collect { chunk ->
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
