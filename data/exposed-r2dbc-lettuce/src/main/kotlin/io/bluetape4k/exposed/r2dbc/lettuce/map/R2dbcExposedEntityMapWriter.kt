package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.redis.lettuce.map.WriteMode
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import java.time.Duration

/**
 * Exposed R2DBC DSL을 사용해 DB에 엔티티를 upsert/delete하는 [R2dbcEntityMapWriter] 구현체.
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입
 * @param table Exposed [IdTable]
 * @param writeMode 쓰기 전략 ([WriteMode])
 * @param updateEntity UPDATE 시 컬럼 매핑 함수
 * @param insertEntity INSERT 시 컬럼 매핑 함수
 * @param chunkSize batchInsert 청크 크기
 * @param retryAttempts 재시도 횟수
 * @param retryInterval 재시도 간격
 */
class R2dbcExposedEntityMapWriter<ID : Comparable<ID>, E : Any>(
    private val table: IdTable<ID>,
    private val writeMode: WriteMode,
    private val updateEntity: (UpdateStatement, E) -> Unit,
    private val insertEntity: (BatchInsertStatement, E) -> Unit,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    retryAttempts: Int = 3,
    retryInterval: Duration = Duration.ofMillis(100),
) : R2dbcEntityMapWriter<ID, E>(
        RetryConfig
            .custom<Any>()
            .maxAttempts(retryAttempts)
            .waitDuration(retryInterval)
            .build()
    ) {
    companion object {
        private const val DEFAULT_CHUNK_SIZE = 1000
    }

    override suspend fun writeEntities(map: Map<ID, E>) {
        if (map.isEmpty() || writeMode == WriteMode.NONE) return

        val existingIds =
            table
                .select(table.id)
                .where { table.id inList map.keys }
                .map { it[table.id].value }
                .toList()
                .toSet()

        for (id in existingIds) {
            table.update({ table.id eq id }) { updateEntity(it, map[id]!!) }
        }

        val newIds = map.keys - existingIds
        if (newIds.isNotEmpty()) {
            for (chunk in newIds.chunked(chunkSize)) {
                table.batchInsert(chunk, shouldReturnGeneratedValues = false) { id ->
                    insertEntity(this, map[id]!!)
                }
            }
        }
    }

    override suspend fun deleteEntities(keys: Collection<ID>) {
        if (keys.isEmpty()) return
        table.deleteWhere { table.id inList keys }
    }
}
