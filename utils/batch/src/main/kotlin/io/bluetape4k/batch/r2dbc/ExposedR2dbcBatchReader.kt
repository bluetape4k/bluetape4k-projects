package io.bluetape4k.batch.r2dbc

import io.bluetape4k.batch.BatchDefaults
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Exposed R2DBC keyset 페이지네이션 기반 [BatchReader] 구현.
 *
 * JDBC 구현([io.bluetape4k.batch.jdbc.ExposedJdbcBatchReader])과 동일한
 * keyset 페이지네이션 패턴을 사용하지만, `suspendTransaction`으로 네이티브 suspend를 활용한다.
 *
 * ## Keyset 페이지네이션 원리
 * - 매 페이지마다 `WHERE keyColumn > lastFetchedKey ORDER BY keyColumn ASC LIMIT pageSize` 쿼리를 실행한다.
 * - OFFSET 방식 대비 대용량에서 성능이 안정적이다.
 *
 * ## 체크포인트 시맨틱
 * - [onChunkCommitted] 호출 시 `lastCommittedKey`를 `lastReadKey`로 전진시킨다.
 * - [restoreFrom] 호출 시 해당 키 이후부터 다시 읽기 시작한다.
 *
 * ```kotlin
 * val reader = ExposedR2dbcBatchReader(
 *     database = db,
 *     table = OrderTable,
 *     keyColumn = OrderTable.id,
 *     pageSize = 500,
 *     rowMapper = { it.toOrderRecord() },
 *     keyExtractor = { it.id },
 * )
 * ```
 *
 * @param K keyset 키 타입 (Comparable — 일반적으로 Long/Int/UUID)
 * @param T 읽어들이는 아이템 타입
 * @param database Exposed R2DBC Database
 * @param table 읽어올 Exposed 테이블
 * @param keyColumn keyset 정렬 기준 컬럼 (PK 권장)
 * @param pageSize 페이지당 조회 크기 (양수)
 * @param rowMapper ResultRow → T 변환 함수
 * @param keyExtractor T → K 키 추출 함수
 */
class ExposedR2dbcBatchReader<K : Comparable<K>, T : Any>(
    private val database: R2dbcDatabase,
    private val table: Table,
    private val keyColumn: Column<K>,
    private val pageSize: Int = BatchDefaults.READER_PAGE_SIZE,
    private val rowMapper: suspend (ResultRow) -> T,
    private val keyExtractor: (T) -> K,
) : BatchReader<T> {

    companion object : KLoggingChannel()

    private val buffer = ArrayDeque<T>()
    private var lastFetchedKey: K? = null
    private var lastReadKey: K? = null
    private var lastCommittedKey: K? = null
    private var exhausted = false

    init {
        pageSize.requirePositiveNumber("pageSize")
    }

    override suspend fun read(): T? {
        if (buffer.isEmpty() && !exhausted) {
            fetchNextPage()
        }
        val item = buffer.removeFirstOrNull() ?: return null
        lastReadKey = keyExtractor(item)
        return item
    }

    override suspend fun checkpoint(): Any? = lastCommittedKey

    override suspend fun onChunkCommitted() {
        lastCommittedKey = lastReadKey
        lastFetchedKey = lastCommittedKey
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun restoreFrom(checkpoint: Any) {
        lastCommittedKey = checkpoint as K
        lastFetchedKey = lastCommittedKey
        lastReadKey = lastCommittedKey
        buffer.clear()
        exhausted = false
    }

    override suspend fun close() {
        runCatching { buffer.clear() }
    }

    private suspend fun fetchNextPage() {
        val page = suspendTransaction(db = database) {
            val query = table.selectAll()
            lastFetchedKey?.let { key ->
                query.andWhere { keyColumn greater key }
            }
            query.orderBy(keyColumn, SortOrder.ASC)
                .limit(pageSize)
                .map { rowMapper(it) }
                .toList()
        }

        if (page.isEmpty()) {
            exhausted = true
        } else {
            buffer.addAll(page)
            lastFetchedKey = keyExtractor(page.last())
        }
    }
}
