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
 * Exposed JDBC DSL을 사용해 DB에 엔티티를 upsert/delete하는 [SuspendedEntityMapWriter] 구현체.
 *
 * ```kotlin
 * val writer = SuspendedExposedEntityMapWriter(
 *     table = ActorTable,
 *     writeMode = WriteMode.WRITE_THROUGH,
 *     updateEntity = { stmt, e -> stmt[ActorTable.name] = e.name },
 *     insertEntity = { stmt, e -> stmt[ActorTable.name] = e.name }
 * )
 * // suspend 컨텍스트(예: runTest, coroutineScope)에서 호출한다
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
class SuspendedExposedEntityMapWriter<ID: Any, E: Any>(
    private val table: IdTable<ID>,
    private val writeMode: WriteMode,
    private val updateEntity: (UpdateStatement, E) -> Unit,
    private val insertEntity: (BatchInsertStatement, E) -> Unit,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    retryAttempts: Int = 3,
    retryInterval: Duration = Duration.ofMillis(100),
): SuspendedEntityMapWriter<ID, E>(
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

    override fun writeEntities(map: Map<ID, E>) {
        if (map.isEmpty() || writeMode == WriteMode.NONE) return

        val existingIds =
            table
                .select(table.id)
                .where { table.id inList map.keys }
                .map { it[table.id].value }
                .toSet()

        existingIds.forEach { id ->
            // WHY: existingIds는 DB에서 map.keys를 기준으로 조회된 ID 집합이므로
            //      map에 반드시 해당 키가 존재한다. !! 연산자 대신 requireNotNull을 사용해
            //      NPE 발생 시 명확한 오류 메시지를 제공한다.
            val entity = requireNotNull(map[id]) { "map에 id=$id 에 해당하는 엔티티가 없습니다" }
            table.update({ table.id eq id }) { updateEntity(it, entity) }
        }

        // WHY: AutoInc 테이블은 DB가 PK를 자동 할당하므로, 클라이언트가 지정한 ID로 INSERT하면
        //      DUPLICATE KEY 오류가 발생할 수 있다. 따라서 AutoInc 테이블에는 신규 INSERT를 건너뛴다.
        val isAutoInc = table.id.autoIncColumnType != null
        val newIds = map.keys - existingIds
        if (newIds.isNotEmpty() && !isAutoInc) {
            newIds.chunked(chunkSize).forEach { chunk ->
                table.batchInsert(chunk) { id ->
                    // WHY: newIds는 map.keys에서 existingIds를 뺀 집합이므로 map에 반드시 존재한다
                    val entity = requireNotNull(map[id]) { "map에 id=$id 에 해당하는 엔티티가 없습니다" }
                    insertEntity(this, entity)
                }
            }
        }
    }

    override fun deleteEntities(keys: Collection<ID>) {
        if (keys.isEmpty()) return
        table.deleteWhere { table.id inList keys }
    }
}
