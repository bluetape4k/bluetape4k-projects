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
 * - 수동 `limit`/`orderBy`가 이미 걸린 Query는 즉시 [IllegalArgumentException]을 던집니다.
 *   [SuspendedQuery] 생성자가 `limit`/`orderByExpressions`를 복사하지 않으므로 여기서 사전 검증합니다.
 */
fun Query.fetchBatchedResultFlow(
    batch: Int = DEFAULT_BATCH_SIZE,
    sortOrder: SortOrder = SortOrder.ASC,
    where: Op<Boolean>? = null,
): Flow<List<ResultRow>> {
    // WHY: SuspendedQuery 생성자는 sourceQuery 의 limit/orderByExpressions 를 복사하지 않습니다.
    // 따라서 이미 limit/orderBy 가 설정된 Query 를 그대로 전달하면 해당 설정이 무시되어
    // 의도하지 않은 배치 결과가 발생할 수 있습니다. caller 에게 즉시 명확한 예외를 전달해
    // 사용 오류를 조기에 발견할 수 있도록 진입점에서 사전 검증합니다.
    require(limit == null) { "A manual `LIMIT` clause should not be set. By default, `batchSize` will be used." }
    require(orderByExpressions.isEmpty()) {
        "A manual `ORDER BY` clause should not be set. By default, the auto-incrementing column will be used."
    }
    return SuspendedQuery(this@fetchBatchedResultFlow, where = where).fetchBatchResultFlow(batch, sortOrder)
}

/**
 * Exposed Query를 커서 기반 배치 조회 [Flow]로 노출하는 Query 구현입니다.
 */
open class SuspendedQuery(
    set: FieldSet,
    where: Op<Boolean>? = null,
): Query(set, where) {

    constructor(
        sourceQuery: Query,
        where: Op<Boolean>? = null,
    ): this(set = sourceQuery.set, where = sourceQuery.where?.and(where ?: Op.TRUE) ?: where)

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
        // WHY: fetchBatchResultFlow 는 내부에서 limit = batchSize 와 orderBy = cursorColumn 을 직접 설정합니다.
        // 외부에서 이미 limit/orderBy 를 지정하면 내부 설정과 충돌하여 페이징이 올바르게 동작하지 않습니다.
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
                is Int  -> autoIncVal.toLong()
                is Long -> autoIncVal
                else    -> throw IllegalArgumentException(
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
                                                else                     -> {
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
                                                else                     -> {
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

                    // WHY: `!!` 연산자 대신 requireNotNull 을 사용해 명확한 에러 메시지를 제공합니다.
                    // cursorColumn 값이 null 이면 다음 배치의 시작 오프셋을 계산할 수 없어 무한루프 또는
                    // 잘못된 결과가 발생합니다. PK 컬럼은 통상 NOT NULL 이지만, nullable Column 을
                    // cursorColumn 으로 잘못 지정한 경우에도 명확한 진단 메시지로 즉시 실패시킵니다.
                    lastOffset = toLong(
                        requireNotNull(results.last().getOrNull(cursorColumn)) {
                            "커서 컬럼(${cursorColumn.name}) 값이 null입니다. NOT NULL 컬럼을 커서로 사용하세요."
                        }
                    )
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
