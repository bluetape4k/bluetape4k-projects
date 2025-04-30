package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.jetbrains.exposed.dao.id.IdTable
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
 * @param writeToDB DB에 데이터를 쓰는 함수입니다.
 * @param deleteFromDB DB에서 데이터를 삭제하는 함수입니다.
 */
abstract class ExposedMapWriter<ID: Any, E: Any>(
    private val writeToDB: (map: Map<ID, E>) -> Unit,
    private val deleteFromDB: (ids: Collection<ID>) -> Unit,
): MapWriter<ID, E> {

    companion object: KLogging()


    override fun write(map: Map<ID, E>) = transaction {
        writeToDB(map)
    }

    override fun delete(keys: Collection<ID>) = transaction {
        deleteFromDB(keys)
    }
}

/**
 * Entity<ID> 를 위한 [ExposedMapWriter] 기본 구현체입니다.
 *
 * @param entityTable Entity<ID> 를 위한 [IdTable] 입니다.
 * @param toEntity ResultRow 를 E 타입으로 변환하는 함수입니다.
 * @param updateBody DB에 이미 존재하는 ID인 경우 UPDATE 하도록 하는 쿼리 입니다.
 * @param batchInsertBody 새로운 엔티티라면 batchInsert 를 수행하도록 하는 쿼리 입니다.
 * @param deleteFromDBOnInvalidate 캐시에서 삭제될 때, DB에서도 삭제할 것인지 여부를 나타냅니다.
 */
open class ExposedEntityMapWriter<ID: Any, E: HasIdentifier<ID>>(
    private val entityTable: IdTable<ID>,
    private val updateBody: IdTable<ID>.(UpdateStatement, E) -> Unit,
    private val batchInsertBody: BatchInsertStatement.(E) -> Unit,
    deleteFromDBOnInvalidate: Boolean = false,
): ExposedMapWriter<ID, E>(
    writeToDB = { map: Map<ID, E> ->
        log.debug { "캐시 변경 사항을 DB에 반영합니다... ids=${map.keys}" }

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
        // id가 DB에서 자동 생성되지 않는 경우에만 batchInsert 를 수행합니다.
        if (!entityTable.id.isDatabaseGenerated()) {
            val entitiesToInsert = map.values.filterNot { it.id in entityIdsToUpdate }
            entityTable.batchInsert(entitiesToInsert) {
                batchInsertBody(this, it)
            }
        }
    },
    deleteFromDB = { ids ->
        if (deleteFromDBOnInvalidate) {
            log.debug { "캐시가 Invalidated 되어, DB에서도 삭제합니다... ids=$ids, id type=${ids.firstOrNull()?.javaClass?.simpleName}" }
            @Suppress("UNCHECKED_CAST")
            val idsToDelete = ids.mapToLanguageType(entityTable.id) as List<ID>

            entityTable.deleteWhere { entityTable.id inList idsToDelete }
        }
    }
) {
    companion object: KLogging()

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
