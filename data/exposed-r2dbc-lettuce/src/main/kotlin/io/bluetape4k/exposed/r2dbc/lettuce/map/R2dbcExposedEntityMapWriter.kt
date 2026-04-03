package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.redis.lettuce.map.WriteMode
import io.bluetape4k.support.requirePositiveNumber
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
 * 코루틴 네이티브로 동작하며, `suspendTransaction` 내에서 실행된다.
 * 기존 PK가 있으면 UPDATE, 없으면 batch INSERT로 처리하는 upsert 전략을 사용한다.
 * Resilience4j retry는 [R2dbcEntityMapWriter] 상위 클래스에서 처리된다.
 *
 * ### 사용 예시
 * ```kotlin
 * val writer = R2dbcExposedEntityMapWriter(
 *     table = UserTable,
 *     writeMode = WriteMode.WRITE_THROUGH,
 *     updateEntity = { stmt, e -> stmt[UserTable.email] = e.email },
 *     insertEntity = { stmt, e -> stmt[UserTable.firstName] = e.firstName },
 * )
 * writer.write(mapOf(1L to user))
 * writer.delete(listOf(2L, 3L))
 * ```
 *
 * @param ID PK 타입 ([Comparable] 구현 필요)
 * @param E 엔티티(DTO) 타입
 * @param table Exposed [IdTable]
 * @param writeMode 쓰기 전략 ([WriteMode]). [WriteMode.NONE]이면 DB에 쓰지 않는다
 * @param updateEntity UPDATE 시 컬럼 매핑 함수 (`(UpdateStatement, E) -> Unit`)
 * @param insertEntity INSERT 시 컬럼 매핑 함수 (`(BatchInsertStatement, E) -> Unit`)
 * @param chunkSize batchInsert 청크 크기 (기본: 1000). 0 이하여서는 안 된다.
 * @param retryAttempts Resilience4j 재시도 횟수 (기본: 3)
 * @param retryInterval Resilience4j 재시도 간격 (기본: 100ms)
 * @see R2dbcEntityMapWriter
 */
class R2dbcExposedEntityMapWriter<ID: Any, E: Any>(
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

    init {
        chunkSize.requirePositiveNumber("chunkSize")
    }

    /**
     * [map]의 엔티티를 DB에 일괄 upsert한다.
     * 기존 PK가 있으면 UPDATE, 없으면 [chunkSize] 단위로 batch INSERT를 수행한다.
     * [WriteMode.NONE]이거나 [map]이 비어 있으면 아무것도 하지 않는다.
     */
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

    /**
     * [keys]에 해당하는 엔티티를 DB에서 일괄 삭제한다.
     * [keys]가 비어 있으면 아무것도 하지 않는다.
     */
    override suspend fun deleteEntities(keys: Collection<ID>) {
        if (keys.isEmpty()) return
        table.deleteWhere { table.id inList keys }
    }
}
