package io.bluetape4k.batch.r2dbc

import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Exposed R2DBC 기반 [BatchWriter] 구현 — 네이티브 suspend.
 *
 * 청크 단위로 `batchInsert`를 수행한다. 빈 리스트는 no-op.
 *
 * ## 사용 예
 * ```kotlin
 * val writer = ExposedR2dbcBatchWriter(
 *     database = db,
 *     table = OrderTable,
 * ) { order: OrderRecord ->
 *     this[OrderTable.customerId] = order.customerId
 *     this[OrderTable.amount] = order.amount
 *     this[OrderTable.createdAt] = order.createdAt
 * }
 * writer.write(listOf(order1, order2, order3))
 * ```
 *
 * @param T 저장할 아이템 타입
 * @param database Exposed R2DBC Database
 * @param table 저장 대상 Exposed 테이블
 * @param bind [BatchInsertStatement]에 아이템 필드를 바인딩하는 람다
 */
class ExposedR2dbcBatchWriter<T : Any>(
    private val database: R2dbcDatabase,
    private val table: Table,
    private val bind: BatchInsertStatement.(T) -> Unit,
) : BatchWriter<T> {

    companion object : KLoggingChannel()

    /**
     * 아이템 목록을 DB에 일괄 삽입한다.
     *
     * @param items 저장할 아이템 목록. 빈 리스트면 no-op.
     */
    override suspend fun write(items: List<T>) {
        if (items.isEmpty()) return
        suspendTransaction(db = database) {
            table.batchInsert(items) { item -> bind(item) }
        }
    }
}
