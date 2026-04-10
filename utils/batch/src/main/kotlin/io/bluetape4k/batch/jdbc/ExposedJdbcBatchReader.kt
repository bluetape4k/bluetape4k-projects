package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.BatchDefaults
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed JDBC keyset 페이지네이션 기반 [BatchReader] 구현.
 *
 * ## Keyset 페이지네이션
 * `WHERE keyColumn > lastFetchedKey ORDER BY keyColumn ASC LIMIT pageSize` 쿼리로
 * 오프셋 방식보다 대용량 데이터에 효율적이다.
 *
 * ## 체크포인트
 * - [checkpoint]: `lastCommittedKey` 반환 (마지막 커밋 성공 키)
 * - [onChunkCommitted]: `lastCommittedKey = lastReadKey` 전진
 * - [restoreFrom]: 저장된 키에서 재개 — [open] 이후, 첫 [read] 전에 호출됨
 *
 * ## 동시성
 * 내부 상태(`buffer`, `lastFetchedKey`, `lastReadKey`, `lastCommittedKey`, `exhausted`)는
 * 단일 스레드(runner)에서 접근하는 것을 전제로 한다.
 *
 * ## 사용 예
 * ```kotlin
 * val reader = ExposedJdbcBatchReader<Long, OrderRecord>(
 *     database = db,
 *     table = OrderTable,
 *     keyColumn = OrderTable.id,
 *     rowMapper = { row -> OrderRecord(row[OrderTable.id].value, row[OrderTable.name]) },
 *     keyExtractor = { it.id },
 * )
 * ```
 *
 * @param K 키 타입 (Comparable)
 * @param T 아이템 타입
 * @param database Exposed JDBC [Database]
 * @param table 대상 [Table]
 * @param keyColumn keyset 기준 컬럼 (`K` 타입)
 * @param pageSize 한 번에 읽어올 페이지 크기 (>0)
 * @param rowMapper [ResultRow] → [T] 변환 함수
 * @param keyExtractor [T] → [K] 추출 함수 (페이지 전진 및 커밋 포인터 갱신에 사용)
 * @param minKey 파티션 시작 키 (exclusive) — null이면 처음부터 읽음. 병렬 처리 시 파티션 하한 설정에 사용.
 * @param maxKey 파티션 종료 키 (inclusive) — null이면 끝까지 읽음. 병렬 처리 시 파티션 상한 설정에 사용.
 */
class ExposedJdbcBatchReader<K: Comparable<K>, T: Any>(
    private val database: Database,
    private val table: Table,
    private val keyColumn: Column<K>,
    private val pageSize: Int = BatchDefaults.READER_PAGE_SIZE,
    private val rowMapper: (ResultRow) -> T,
    private val keyExtractor: (T) -> K,
    private val minKey: K? = null,
    private val maxKey: K? = null,
): BatchReader<T> {

    companion object: KLoggingChannel()

    init {
        pageSize.requirePositiveNumber("pageSize")
    }

    private val buffer = ArrayDeque<T>()
    private var lastFetchedKey: K? = minKey
    private var lastReadKey: K? = null
    private var lastCommittedKey: K? = null
    private var exhausted = false

    override suspend fun open() {
        buffer.clear()
        lastFetchedKey = minKey
        lastReadKey = null
        lastCommittedKey = null
        exhausted = false
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
        log.debug { "청크 커밋 완료: lastCommittedKey=$lastCommittedKey" }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun restoreFrom(checkpoint: Any) {
        val key = checkpoint as K
        lastCommittedKey = key
        lastFetchedKey = key
        lastReadKey = key
        buffer.clear()
        exhausted = false
        log.debug { "체크포인트 복원: lastCommittedKey=$key" }
    }

    override suspend fun close() {
        runCatching { buffer.clear() }
    }

    private suspend fun fetchNextPage() {
        val page = withContext(Dispatchers.VT) {
            transaction(database) {
                val query = table.selectAll()
                lastFetchedKey?.let { key ->
                    query.andWhere { keyColumn greater key }
                }
                maxKey?.let { max ->
                    query.andWhere { keyColumn lessEq max }
                }
                query.orderBy(keyColumn, SortOrder.ASC)
                    .limit(pageSize)
                    .map(rowMapper)
            }
        }

        if (page.isEmpty()) {
            exhausted = true
            log.debug { "페이지 조회 결과 없음 — 소진 상태로 전환" }
        } else {
            buffer.addAll(page)
            lastFetchedKey = keyExtractor(page.last())
            log.debug { "페이지 조회: size=${page.size}, lastFetchedKey=$lastFetchedKey" }
        }
    }
}
