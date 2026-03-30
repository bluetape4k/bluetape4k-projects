package io.bluetape4k.exposed.jdbc

import io.bluetape4k.collections.toList
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
 * 배치 조회의 기본 크기입니다.
 */
private const val DEFAULT_BATCH_SIZE: Int = 1000

/**
 * [FieldSet]을 배치 단위 [Flow] 조회로 변환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [Query.fetchBatchedResultFlow]를 호출합니다.
 * - 현재 [FieldSet]의 선택 컬럼 구성을 그대로 유지합니다.
 * - 기준 컬럼은 첫 번째 select 컬럼이며 Int/Long/EntityID(Int|Long)만 지원합니다.
 *
 * ```kotlin
 * val flow = ProductTable.select(ProductTable.id).fetchBatchedResultFlow(batch = 100)
 * // flow != null
 * ```
 */
fun FieldSet.fetchBatchedResultFlow(
    batch: Int = DEFAULT_BATCH_SIZE,
    sortOrder: SortOrder = SortOrder.ASC,
    where: Op<Boolean>? = null,
): Flow<List<ResultRow>> = Query(this, where = where).fetchBatchedResultFlow(batch, sortOrder)

/**
 * [Query]를 배치 단위 [Flow] 조회로 변환합니다.
 *
 * ## 동작/계약
 * - 현재 Query의 `set`, 기존 `where`, 추가 `where`를 함께 사용해 [SuspendedQuery]를 만들고 배치 조회를 수행합니다.
 * - 추가 `where`가 있으면 기존 조건과 `AND`로 결합됩니다.
 * - 수동 `limit`/`orderBy`가 이미 걸린 Query는 [SuspendedQuery.fetchBatchResultFlow]에서 거부됩니다.
 */
fun Query.fetchBatchedResultFlow(
    batch: Int = DEFAULT_BATCH_SIZE,
    sortOrder: SortOrder = SortOrder.ASC,
    where: Op<Boolean>? = null,
): Flow<List<ResultRow>> =
    SuspendedQuery(this@fetchBatchedResultFlow, where = where).fetchBatchResultFlow(batch, sortOrder)

/**
 * Exposed Query를 커서 기반 배치 조회 [Flow]로 노출하는 Query 구현입니다.
 */
open class SuspendedQuery(
    set: FieldSet,
    where: Op<Boolean>? = null,
) : Query(set, where) {

    constructor(
        sourceQuery: Query,
        where: Op<Boolean>? = null,
    ) : this(set = sourceQuery.set, where = sourceQuery.where?.and(where ?: Op.TRUE) ?: where)

    /**
     * 결과를 `batchSize` 단위로 끊어 [Flow]로 방출합니다.
     *
     * ## 동작/계약
     * - `batchSize <= 0`, 수동 `limit`, 수동 `orderBy`가 있으면 [IllegalArgumentException]이 발생합니다.
     * - 첫 번째 컬럼 타입이 Int/Long/EntityID(Int|Long)가 아니면 [IllegalArgumentException]이 발생합니다.
     * - `limit`/`orderBy` 변이는 flow 수집 시점에만 발생하며, 수집 완료 또는 취소 시 `finally`에서 원복합니다.
     * - 각 배치는 새 `List<ResultRow>`로 방출됩니다.
     *
     * ```kotlin
     * val batches = ProductTable.select(ProductTable.id).fetchBatchedResultFlow(500)
     * // batches는 조건에 맞는 결과를 500건씩 방출
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun fetchBatchResultFlow(
        batchSize: Int = DEFAULT_BATCH_SIZE,
        sortOrder: SortOrder = SortOrder.ASC,
    ): Flow<List<ResultRow>> {
        batchSize.requirePositiveNumber("batchSize")
        require(limit == null) { "A manual `LIMIT` clause should not be set. By default, `batchSize` will be used." }
        require(orderByExpressions.isEmpty()) {
            "A manual `ORDER BY` clause should not be set. By default, the auto-incrementing column will be used."
        }

        // snowflakeId 같은 Global Unique ID 도 지원하기 위해 첫 번째 선택 컬럼을 커서로 사용
        val cursorColumn =
            try {
                set.fields.first { it is Column<*> } as Column<*>
            } catch (_: NoSuchElementException) {
                throw UnsupportedOperationException(
                    "Batched select only works when the first selected expression is an Int/Long id column"
                )
            }
        val columnType = cursorColumn.columnType
        require(
            columnType is IntegerColumnType ||
                columnType is LongColumnType ||
                columnType is EntityIDColumnType<*>
        ) {
            "Batched select only supports Int/Long id columns. (column=${cursorColumn.name})"
        }

        val whereOp = where ?: Op.TRUE
        val fetchInAscendingOrder =
            sortOrder in listOf(SortOrder.ASC, SortOrder.ASC_NULLS_FIRST, SortOrder.ASC_NULLS_LAST)

        fun toLong(autoIncVal: Any): Long =
            when (autoIncVal) {
                is EntityID<*> -> toLong(autoIncVal.value)
                is Int -> autoIncVal.toLong()
                is Long -> autoIncVal
                else -> throw IllegalArgumentException(
                    "Batched select only supports Int/Long id but was ${autoIncVal::class.qualifiedName}"
                )
            }

        return flow {
            // limit/orderBy 변이를 flow 수집 시점으로 지연시켜
            // fetchBatchResultFlow() 호출 시점에는 원본 Query를 변경하지 않습니다.
            val originalLimit = this@SuspendedQuery.limit
            val originalOrderBy = this@SuspendedQuery.orderByExpressions.toList()
            try {
                this@SuspendedQuery.limit = batchSize
                (this@SuspendedQuery.orderByExpressions as MutableList).add(cursorColumn to sortOrder)
                var lastOffset = if (fetchInAscendingOrder) 0L else null
                while (true) {
                    val query =
                        this@SuspendedQuery.copy().adjustWhere {
                            lastOffset?.let { lastOffset ->
                                whereOp and
                                    if (fetchInAscendingOrder) {
                                        when (cursorColumn.columnType) {
                                            is EntityIDColumnType<*> -> {
                                                (cursorColumn as? Column<EntityID<Long>>)?.let {
                                                    (it greater lastOffset)
                                                } ?: (cursorColumn as? Column<EntityID<Int>>)?.let {
                                                    (it greater lastOffset.toInt())
                                                } ?: (cursorColumn greater lastOffset)
                                            }
                                            else -> {
                                                (cursorColumn greater lastOffset)
                                            }
                                        }
                                    } else {
                                        when (cursorColumn.columnType) {
                                            is EntityIDColumnType<*> -> {
                                                (cursorColumn as? Column<EntityID<Long>>)?.let {
                                                    (it less lastOffset)
                                                } ?: (cursorColumn as? Column<EntityID<Int>>)?.let {
                                                    (it less lastOffset.toInt())
                                                } ?: (cursorColumn less lastOffset)
                                            }
                                            else -> {
                                                (cursorColumn less lastOffset)
                                            }
                                        }
                                    }
                            } ?: whereOp
                        }
                    val results = query.iterator().toList()
                    if (results.isNotEmpty()) {
                        emit(results)
                    }
                    if (results.size < batchSize) break

                    lastOffset = toLong(results.last()[cursorColumn]!!)
                }
            } finally {
                this@SuspendedQuery.limit = originalLimit
                (this@SuspendedQuery.orderByExpressions as MutableList).apply {
                    clear()
                    addAll(originalOrderBy)
                }
            }
        }
    }
}
