package io.bluetape4k.exposed.bigquery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.DecimalColumnType
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.javatime.JavaInstantColumnType
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

/**
 * Exposed [Query] 객체를 BigQuery REST API로 실행하는 실행기.
 *
 * [BigQueryContext.withBigQuery]를 통해 생성됩니다.
 *
 * ```kotlin
 * with(context) {
 *     // 동기
 *     val rows = Events.selectAll().where { Events.region eq "kr" }.withBigQuery().toList()
 *
 *     // 비동기 (suspend)
 *     val rows = Events.selectAll().withBigQuery().toListSuspending()
 *
 *     // 스트리밍 (Flow, 대용량 결과셋에 적합)
 *     Events.selectAll().withBigQuery().toFlow().collect { row -> ... }
 * }
 * ```
 */
class BigQueryQueryExecutor(
    private val query: Query,
    private val context: BigQueryContext,
) {
    private fun sql(): String = transaction(context.sqlGenDb) { query.prepareSQL(this, prepared = false) }

    /** 쿼리를 실행하고 전체 결과(페이지네이션 포함)를 [BigQueryResultRow] 목록으로 반환합니다. */
    fun toList(): List<BigQueryResultRow> = context.collectAllRows(sql())

    /** 쿼리를 비동기로 실행하고 전체 결과를 반환합니다. [BigQueryContext.dispatcher]에서 블로킹 REST 호출을 수행합니다. */
    suspend fun toListSuspending(): List<BigQueryResultRow> = withContext(context.dispatcher) { toList() }

    /**
     * 결과를 행 단위로 emit하는 [Flow]를 반환합니다.
     * 내부적으로는 BigQuery pageToken을 따라가며 각 페이지를 순차 조회한 뒤,
     * 각 페이지의 행을 즉시 emit 하므로 전체 결과를 한 번에 적재하지 않습니다.
     */
    fun toFlow(): Flow<BigQueryResultRow> = context.collectRowsFlow(sql())

    /** 결과가 정확히 1건임을 기대합니다. 0건이거나 2건 이상이면 예외를 던집니다. */
    fun single(): BigQueryResultRow = toList().single()

    /** 결과가 0건이면 null, 1건이면 해당 행을 반환합니다. 2건 이상이면 예외를 던집니다. */
    fun singleOrNull(): BigQueryResultRow? = toList().singleOrNull()

    /** 결과의 첫 번째 행을 반환합니다. 결과가 없으면 null을 반환합니다. */
    fun firstOrNull(): BigQueryResultRow? = toList().firstOrNull()
}

/**
 * BigQuery REST API 응답의 단일 행.
 *
 * Exposed [Column] 참조로 타입 안전하게 값을 읽을 수 있습니다.
 * 내부 맵 키는 소문자로 정규화하므로 컬럼 이름 조회는 대소문자를 구분하지 않습니다.
 *
 * ```kotlin
 * val row: BigQueryResultRow = ...
 * val region: String      = row[Events.region]
 * val userId: Long        = row[Events.userId]
 * val amount: BigDecimal? = row[Events.amount]
 * ```
 */
class BigQueryResultRow(private val data: Map<String, Any?>) {
    private val normalizedData: Map<String, Any?> = data.mapKeys { (name, _) -> name.lowercase(Locale.ROOT) }

    /** Exposed [Column]으로 타입 변환된 값을 반환합니다. */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(column: Column<T>): T =
        convertValue(normalizedData[column.name.lowercase(Locale.ROOT)], column) as T

    /** 컬럼 이름으로 원시값을 반환합니다. */
    operator fun get(name: String): Any? = normalizedData[name.lowercase(Locale.ROOT)]

    @Suppress("UNCHECKED_CAST")
    private fun <T> convertValue(raw: Any?, column: Column<T>): T? {
        if (raw == null || raw.javaClass == Any::class.java) return null
        val s = raw.toString()
        if (s.equals("null", ignoreCase = true)) return null
        return when (column.columnType) {
            is DecimalColumnType ->
                BigDecimal(s)
            is JavaInstantColumnType ->
                // BigQuery REST API: TIMESTAMP = 초 단위 float 문자열 (예: "1.704067200E9")
                Instant.ofEpochMilli((s.toDouble() * 1000).toLong())
            else ->
                column.columnType.valueFromDB(s)
        } as T?
    }

    override fun toString(): String = normalizedData.toString()
}
