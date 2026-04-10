package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed JDBC 기반 [BatchWriter] 구현.
 *
 * 청크 단위로 [batchInsert]를 수행한다. 빈 리스트는 no-op.
 *
 * ## 사용 예
 * ```kotlin
 * val writer = ExposedJdbcBatchWriter(
 *     database = db,
 *     table = OrderTable,
 *     bind = { order ->
 *         this[OrderTable.id]   = order.id
 *         this[OrderTable.name] = order.name
 *     },
 * )
 * ```
 *
 * @param T 아이템 타입
 * @param database Exposed JDBC [Database]
 * @param table 대상 [Table]
 * @param ignore `INSERT IGNORE` 사용 여부 (중복 무시)
 * @param bind 아이템 → 컬럼 바인딩 함수
 */
class ExposedJdbcBatchWriter<T: Any>(
    private val database: Database,
    private val table: Table,
    private val ignore: Boolean = false,
    private val bind: BatchInsertStatement.(T) -> Unit,
): BatchWriter<T> {

    companion object: KLoggingChannel()

    override suspend fun write(items: List<T>) {
        if (items.isEmpty()) return
        withContext(Dispatchers.VT) {
            transaction(database) {
                table.batchInsert(items, ignore = ignore) { item -> bind(item) }
            }
        }
        log.debug { "batchInsert 완료: table=${table.tableName}, count=${items.size}" }
    }
}
