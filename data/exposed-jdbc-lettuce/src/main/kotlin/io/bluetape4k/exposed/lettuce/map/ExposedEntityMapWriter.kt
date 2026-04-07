package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.redis.lettuce.map.WriteMode
import io.bluetape4k.support.requirePositiveNumber
import io.github.resilience4j.retry.RetryConfig
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Duration

/**
 * Exposed DSL을 사용해 DB에 엔티티를 upsert/delete하는 [EntityMapWriter] 구현체.
 *
 * ```kotlin
 * val writer = ExposedEntityMapWriter(
 *     table = ActorTable,
 *     writeMode = WriteMode.WRITE_THROUGH,
 *     updateEntity = { stmt, e -> stmt[ActorTable.name] = e.name },
 *     insertEntity = { stmt, e -> stmt[ActorTable.name] = e.name }
 * )
 * writer.write(mapOf(1L to ActorRecord(id = 1L, name = "Alice")))
 * ```
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입
 * @param table Exposed [IdTable]
 * @param writeMode 쓰기 전략 ([WriteMode])
 * @param updateEntity UPDATE 시 컬럼 매핑 함수
 * @param insertEntity INSERT 시 컬럼 매핑 함수
 * @param chunkSize batchInsert 청크 크기. 0 이하여서는 안 된다.
 * @param retryAttempts 재시도 횟수
 * @param retryInterval 재시도 간격
 */
class ExposedEntityMapWriter<ID: Any, E: Any>(
    private val table: IdTable<ID>,
    private val writeMode: WriteMode,
    private val updateEntity: (UpdateStatement, E) -> Unit,
    private val insertEntity: (BatchInsertStatement, E) -> Unit,
    private val chunkSize: Int = 1000,
    retryAttempts: Int = 3,
    retryInterval: Duration = Duration.ofMillis(100),
) : EntityMapWriter<ID, E>(
        RetryConfig
            .custom<Any>()
            .maxAttempts(retryAttempts)
            .waitDuration(retryInterval)
            .build()
    ) {
    init {
        chunkSize.requirePositiveNumber("chunkSize")
    }

    override fun writeEntities(map: Map<ID, E>) {
        if (map.isEmpty() || writeMode == WriteMode.NONE) return

        val existingIds =
            table
                .select(table.id)
                .where { table.id inList map.keys }
                .map { it[table.id].value }
                .toSet()

        existingIds.forEach { id ->
            table.update({ table.id eq id }) { updateEntity(it, map[id]!!) }
        }

        // AutoInc 테이블의 경우 DB가 ID를 할당하므로 클라이언트 지정 ID로 삽입하지 않는다
        val isAutoInc = table.id.autoIncColumnType != null
        val newIds = map.keys - existingIds
        if (newIds.isNotEmpty() && !isAutoInc) {
            newIds.chunked(chunkSize).forEach { chunk ->
                table.batchInsert(chunk) { id -> insertEntity(this, map[id]!!) }
            }
        }
    }

    override fun deleteEntities(keys: Collection<ID>) {
        if (keys.isEmpty()) return
        table.deleteWhere { table.id inList keys }
    }
}
