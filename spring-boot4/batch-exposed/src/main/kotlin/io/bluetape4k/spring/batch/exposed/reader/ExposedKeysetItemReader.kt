package io.bluetape4k.spring.batch.exposed.reader

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.spring.batch.exposed.support.castToLong
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemStreamReader
import org.springframework.beans.factory.InitializingBean

/**
 * Keyset 기반 페이지 읽기 [ItemStreamReader].
 *
 * - `WHERE [column] > lastKey AND [column] <= maxId ORDER BY [column] ASC LIMIT [pageSize]`
 * - lastKey를 [ExecutionContext]에 저장하여 restart 시 마지막 위치부터 재개
 * - 파티션별 독립 인스턴스이므로 thread-safety 보장
 * - synchronized `read()` 구현 (Spring Batch 컨벤션 준수)
 *
 * @param T 반환 타입
 * @param database Exposed [Database] (null이면 SpringTransactionManager 현재 트랜잭션 참여)
 * @param pageSize 한 번에 읽을 레코드 수 (기본값: 500)
 * @param column keyset 기준 컬럼. `Column<Long>` 또는 `castTo<Long>()` 결과 모두 허용
 * @param table Exposed [Table]
 * @param rowMapper [ResultRow] -> T 변환 함수
 * @param keyExtractor [ResultRow]에서 keyset 컬럼 Long 값 추출
 * @param additionalCondition 추가 WHERE 조건 람다 (null이면 조건 없음)
 */
open class ExposedKeysetItemReader<T : Any>(
    private val database: Database? = null,
    private val pageSize: Int = 500,
    private val column: ExpressionWithColumnType<Long>,
    private val table: Table,
    private val rowMapper: (ResultRow) -> T,
    private val keyExtractor: (ResultRow) -> Long = { it[column] },
    private val additionalCondition: (() -> Op<Boolean>)? = null,
) : ItemStreamReader<T>, InitializingBean {

    companion object : KLogging() {
        private const val LAST_KEY = "lastKey"

        /**
         * `LongIdTable.id` (`Column<EntityID<Long>>`) 기반 Reader 팩토리.
         *
         * - `column`: `table.id.castTo<Long>(LongColumnType())`으로 Long 변환 — WHERE/ORDER BY에서 Long 비교 사용
         * - `keyExtractor`: `it[table.id].value`로 EntityID에서 Long 추출 (selectAll 결과에서 원본 id 컬럼 사용)
         */
        fun <T : Any> forEntityId(
            table: IdTable<Long>,
            pageSize: Int = 500,
            rowMapper: (ResultRow) -> T,
            keyExtractor: (ResultRow) -> Long = { it[table.id].value },
            additionalCondition: (() -> Op<Boolean>)? = null,
            database: Database? = null,
        ): ExposedKeysetItemReader<T> = ExposedKeysetItemReader(
            database = database,
            pageSize = pageSize,
            // MySQL은 CAST(id AS BIGINT)를 지원하지 않으므로 SIGNED를 사용하는 dialect-aware cast 사용
            column = table.id.castToLong(),
            table = table,
            rowMapper = rowMapper,
            keyExtractor = keyExtractor,
            additionalCondition = additionalCondition,
        )
    }

    private var minId: Long = 0L
    private var maxId: Long = Long.MAX_VALUE
    private var lastKey: Long = 0L
    // (key, item) 쌍 저장 — lastKey를 소비 시점에 업데이트하기 위해 키와 아이템을 함께 보관
    private val buffer: MutableList<Pair<Long, T>> = mutableListOf()
    private var bufferIndex: Int = 0
    private var exhausted: Boolean = false

    override fun afterPropertiesSet() {
        require(pageSize > 0) { "pageSize must be positive" }
    }

    override fun open(executionContext: ExecutionContext) {
        minId = executionContext.getLong(ExposedRangePartitioner.PARTITION_MIN_ID)
        maxId = executionContext.getLong(ExposedRangePartitioner.PARTITION_MAX_ID)

        lastKey = if (executionContext.containsKey(LAST_KEY)) {
            executionContext.getLong(LAST_KEY)
        } else {
            minId - 1
        }
    }

    @Synchronized
    override fun read(): T? {
        if (exhausted) return null

        if (bufferIndex >= buffer.size) {
            fetchNextPage()
            if (buffer.isEmpty()) {
                exhausted = true
                return null
            }
        }

        val (key, item) = buffer[bufferIndex++]
        lastKey = key
        return item
    }

    override fun update(executionContext: ExecutionContext) {
        executionContext.putLong(LAST_KEY, lastKey)
    }

    override fun close() {
        buffer.clear()
        bufferIndex = 0
        exhausted = false
    }

    private fun fetchNextPage() {
        buffer.clear()
        bufferIndex = 0

        transaction(database) {
            var condition: Op<Boolean> = (column greater lastKey) and (column lessEq maxId)
            additionalCondition?.let { addCond ->
                condition = condition and addCond()
            }

            val resultRows = table.selectAll()
                .where { condition }
                .orderBy(column, SortOrder.ASC)
                .limit(pageSize)
                .toList()

            buffer.addAll(resultRows.map { row -> keyExtractor(row) to rowMapper(row) })
        }

        log.debug { "${buffer.size}건 읽음 (table=${table.tableName}, lastKey=$lastKey)" }
    }
}
