package io.bluetape4k.exposed.core

import io.bluetape4k.collections.toList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.EntityIDColumnType
import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Query

/**
 * [FieldSet] 에서 [SuspendedQuery.fetchBatchedResultFlow] 메소드를 코루틴 환경에서 사용할 수 있도록 확장한 함수입니다.
 *
 * 이 함수를 사용하려면, 조회하는 첫번째 컬럼이 Int, Long 수형이어야 합니다.
 *
 * ```
 * // 10개씩 배치로 읽어온다
 * val batchedIds: List<List<Int>> = ProductTable
 *     .fetchBatchResultFlow(10)
 *     .buffer(capacity = 2)
 *     .map { rows -> rows.map { it[ProductTable.id].value } }
 *     .toList()
 * ```
 */
fun FieldSet.fetchBatchedResultFlow(
    batch: Int = 1000,
    sortOrder: SortOrder = SortOrder.ASC,
    where: Op<Boolean>? = null,
): Flow<List<ResultRow>> = Query(this.source, where = where).fetchBatchedResultFlow(batch, sortOrder)

/**
 * [SuspendedQuery.fetchBatchedResultFlow] 메소드를 코루틴 환경에서 사용할 수 있도록 확장한 함수입니다.
 *
 * 이 함수를 사용하려면, 조회하는 첫번째 컬럼이 Int, Long 수형이어야 합니다.
 *
 * ```
 * // 10개씩 배치로 읽어온다
 * val batchedIds: List<List<Int>> = ProductTable
 *     .select(ProductTable.id)
 *     .fetchBatchResultFlow(10)
 *     .buffer(capacity = 2)
 *     .map { rows -> rows.map { it[ProductTable.id].value } }
 *     .toList()
 * ```
 */
fun Query.fetchBatchedResultFlow(
    batch: Int = 1000,
    sortOrder: SortOrder = SortOrder.ASC,
    where: Op<Boolean>? = null,
): Flow<List<ResultRow>> =
    SuspendedQuery(this@fetchBatchedResultFlow.set, where = where).fetchBatchResultFlow(batch, sortOrder)

/**
 * [Query.fetchBatchedResults] 메소드를 코루틴 환경에서 사용할 수 있도록 확장한 함수를 제공하는 클래스입니다.
 */
open class SuspendedQuery(set: FieldSet, where: Op<Boolean>? = null): Query(set, where) {

    /**
     * [Query.fetchBatchedResults] 메소드를 코루틴 환경에서 사용할 수 있도록 확장한 메소드입니다.
     *
     * 이 함수를 사용하려면, 조회하는 첫번째 컬럼이 Int, Long 수형이어야 합니다.
     *
     * ```
     * // 10개씩 배치로 읽어온다
     * val batchedIds: List<List<Int>> = ProductTable
     *     .select(ProductTable.id)
     *     .fetchBatchResultFlow(10)
     *     .buffer(capacity = 2)
     *     .map { rows -> rows.map { it[ProductTable.id].value } }
     *     .toList()
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun fetchBatchResultFlow(batchSize: Int = 1000, sortOrder: SortOrder = SortOrder.ASC): Flow<List<ResultRow>> {
        require(batchSize > 0) { "Batch size should be greater than 0." }
        require(limit == null) { "A manual `LIMIT` clause should not be set. By default, `batchSize` will be used." }
        require(orderByExpressions.isEmpty()) {
            "A manual `ORDER BY` clause should not be set. By default, the auto-incrementing column will be used."
        }

        val comparatedColumn = try {
            set.source.columns.first()  //  { it.columnType.isAutoInc } // snowflakeId 같은 Global Unique ID 도 지원하기 위해
        } catch (_: NoSuchElementException) {
            throw UnsupportedOperationException("Batched select only works on tables with an auto-incrementing column")
        }
        val columnType = comparatedColumn.columnType
        require(
            columnType is IntegerColumnType ||
                    columnType is LongColumnType ||
                    columnType is EntityIDColumnType<*>
        ) {
            "Batched select only supports Int/Long id columns. (column=${comparatedColumn.name})"
        }

        val originalLimit = limit
        val originalOrderBy = orderByExpressions.toList()
        limit = batchSize
        (orderByExpressions as MutableList).add(comparatedColumn to sortOrder)
        val whereOp = where ?: Op.TRUE
        val fetchInAscendingOrder =
            sortOrder in listOf(SortOrder.ASC, SortOrder.ASC_NULLS_FIRST, SortOrder.ASC_NULLS_LAST)

        fun toLong(autoIncVal: Any): Long = when (autoIncVal) {
            is EntityID<*> -> toLong(autoIncVal.value)
            is Int -> autoIncVal.toLong()
            is Long -> autoIncVal
            else    -> throw IllegalArgumentException(
                "Batched select only supports Int/Long id but was ${autoIncVal::class.qualifiedName}"
            )
        }

        return channelFlow {
            try {
                var lastOffset = if (fetchInAscendingOrder) 0L else null
                while (true) {
                    val query = this@SuspendedQuery.copy().adjustWhere {
                        lastOffset?.let { lastOffset ->
                            whereOp and if (fetchInAscendingOrder) {
                                when (comparatedColumn.columnType) {
                                    is EntityIDColumnType<*> -> {
                                        (comparatedColumn as? Column<EntityID<Long>>)?.let {
                                            (it greater lastOffset)
                                        } ?: (comparatedColumn as? Column<EntityID<Int>>)?.let {
                                            (it greater lastOffset.toInt())
                                        } ?: (comparatedColumn greater lastOffset)
                                    }
                                    else                     -> (comparatedColumn greater lastOffset)
                                }
                            } else {
                                when (comparatedColumn.columnType) {
                                    is EntityIDColumnType<*> -> {
                                        (comparatedColumn as? Column<EntityID<Long>>)?.let {
                                            (it less lastOffset)
                                        } ?: (comparatedColumn as? Column<EntityID<Int>>)?.let {
                                            (it less lastOffset.toInt())
                                        } ?: (comparatedColumn less lastOffset)
                                    }
                                    else                     -> (comparatedColumn less lastOffset)
                                }
                            }
                        } ?: whereOp
                    }
                    val results = query.iterator().toList()
                    if (results.isNotEmpty()) {
                        send(results)
                    }
                    if (results.size < batchSize) break

                    lastOffset = toLong(results.last()[comparatedColumn]!!)
                }
            } finally {
                limit = originalLimit
                (orderByExpressions as MutableList).apply {
                    clear()
                    addAll(originalOrderBy)
                }
            }
        }
    }
}
