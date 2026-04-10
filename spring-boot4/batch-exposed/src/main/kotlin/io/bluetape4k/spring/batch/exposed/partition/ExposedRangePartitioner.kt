package io.bluetape4k.spring.batch.exposed.partition

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.support.castToLong
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.batch.core.partition.Partitioner
import org.springframework.batch.infrastructure.item.ExecutionContext

/**
 * auto-increment PK/sequence `Column<Long>` 컬럼 기반으로 ID 범위를 N개 파티션으로 분할하는 [Partitioner].
 *
 * 전제 조건:
 * - [column]은 `ExpressionWithColumnType<Long>` 타입의 unique + auto-increment 또는 monotonic sequence여야 함
 * - `LongIdTable.id` (`Column<EntityID<Long>>`) 사용 시 `ExposedRangePartitioner.forEntityId(table)` 팩토리 사용
 * - 실행 중 대규모 insert/delete가 없는 상황에 적합
 *
 * 사용 예시:
 * ```kotlin
 * // Column<Long> 컬럼 직접 사용
 * val partitioner = ExposedRangePartitioner(
 *     table = SourceTable,
 *     column = SourceTable.longId,
 *     gridSize = 16,
 * )
 *
 * // LongIdTable (Column<EntityID<Long>>) 사용 시: forEntityId() 팩토리
 * val partitioner = ExposedRangePartitioner.forEntityId(
 *     table = SourceTable,
 *     gridSize = 16,
 * )
 * ```
 *
 * @param database Exposed [Database] (null이면 SpringTransactionManager 활용)
 * @param table 파티션 대상 Exposed [Table]
 * @param column 분할 기준 컬럼 (PK, auto-increment). `Column<Long>` 또는 `castTo<Long>()` 결과 모두 허용
 * @param gridSize 파티션 수 (기본값: 8)
 * @param selectMinMax min/max 조회 람다 (커스터마이징 가능)
 */
class ExposedRangePartitioner(
    private val database: Database? = null,
    private val table: Table,
    private val column: ExpressionWithColumnType<Long>,
    private val gridSize: Int = 8,
    private val selectMinMax: Transaction.() -> Pair<Long?, Long?> = {
        val minExpr = column.min()
        val maxExpr = column.max()
        table.select(minExpr, maxExpr).single()
            .let { it[minExpr] to it[maxExpr] }
    },
) : Partitioner {

    companion object : KLogging() {
        /** ExecutionContext에 저장되는 파티션 시작 ID 키 */
        const val PARTITION_MIN_ID = "minId"

        /** ExecutionContext에 저장되는 파티션 종료 ID 키 */
        const val PARTITION_MAX_ID = "maxId"

        /**
         * `LongIdTable.id` (`Column<EntityID<Long>>`) 기반 파티셔너 팩토리.
         *
         * - `column`: `table.id.castTo<Long>(LongColumnType())`으로 Long 변환 — WHERE 절에서 Long 비교 사용
         * - `selectMinMax`: castTo 컬럼으로 min/max 조회 — EntityID 래핑 없이 Long 직접 반환
         *
         * ```kotlin
         * val partitioner = ExposedRangePartitioner.forEntityId(
         *     table = SourceTable,
         *     gridSize = 16,
         * )
         * ```
         */
        fun forEntityId(
            table: IdTable<Long>,
            gridSize: Int = 8,
            database: Database? = null,
        ): ExposedRangePartitioner {
            val castCol = table.id.castToLong()
            return ExposedRangePartitioner(
                database = database,
                table = table,
                column = castCol,
                gridSize = gridSize,
                selectMinMax = {
                    // table.id.min()/.max()는 CAST 없이 직접 MIN(id)/MAX(id)를 생성 — 모든 DB 호환
                    val minExpr = table.id.min()
                    val maxExpr = table.id.max()
                    table.select(minExpr, maxExpr).single()
                        .let { row -> row[minExpr]?.value to row[maxExpr]?.value }
                },
            )
        }
    }

    override fun partition(gridSize: Int): Map<String, ExecutionContext> {
        val effectiveGridSize = if (gridSize > 0) gridSize else this.gridSize

        val (min, max) = transaction(database) { selectMinMax() }

        if (min == null || max == null) {
            return mapOf("partition-0" to ExecutionContext().apply {
                putLong(PARTITION_MIN_ID, 0L)
                putLong(PARTITION_MAX_ID, -1L)
            })
        }

        val totalRange = max - min + 1
        val safeGridSize = minOf(effectiveGridSize.toLong(), totalRange.coerceAtLeast(1L)).toInt()
        val rangeSize = totalRange / safeGridSize

        return (0 until safeGridSize).associate { i ->
            val partMinId = min + i * rangeSize
            val partMaxId = if (i == safeGridSize - 1) max else min + (i + 1) * rangeSize - 1

            "partition-$i" to ExecutionContext().apply {
                putLong(PARTITION_MIN_ID, partMinId)
                putLong(PARTITION_MAX_ID, partMaxId)
            }
        }
    }
}
