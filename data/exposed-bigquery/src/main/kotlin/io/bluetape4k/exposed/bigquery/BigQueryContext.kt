package io.bluetape4k.exposed.bigquery

import com.google.api.services.bigquery.Bigquery
import com.google.api.services.bigquery.model.DatasetReference
import com.google.api.services.bigquery.model.QueryRequest
import com.google.api.services.bigquery.model.QueryResponse
import com.google.api.services.bigquery.model.TableRow
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.DeleteStatement
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.Statement
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * **Exposed SQL generator + BigQuery REST executor** 컨텍스트.
 *
 * Exposed DSL로 만든 Query를 SQL로 변환(H2 PostgreSQL 모드)한 뒤 BigQuery REST API로 실행합니다.
 * JDBC 드라이버 없이 `google-api-services-bigquery-v2`를 사용합니다.
 *
 * ## 포지셔닝
 *
 * - **보장**: SELECT/filter/order/group/aggregate, 기본 DML(INSERT/UPDATE/DELETE)
 * - **제한**: SchemaUtils DDL 자동화, DAO 완전 호환, JDBC 트랜잭션 의미론
 * - **조건부**: join/alias(컬럼명 기준 접근), 대용량 결과셋(pagination 자동 처리)
 *
 * ## 동기 사용 예
 *
 * ```kotlin
 * val context = BigQueryContext.create(bigquery, projectId = "my-project", datasetId = "my-dataset")
 *
 * with(context) {
 *     val rows = Events.selectAll().where { Events.region eq "kr" }.withBigQuery().toList()
 *     Events.execInsert { it[eventId] = 1L; it[region] = "kr" }
 *     Events.execUpdate(Events.region eq "kr") { it[eventType] = "UPDATED" }
 *     Events.execDelete(Events.region eq "us")
 * }
 * ```
 *
 * ## 코루틴 사용 예
 *
 * ```kotlin
 * with(context) {
 *     // suspend — IO 스레드에서 블로킹 REST 호출
 *     val rows = Events.selectAll().where { Events.region eq "kr" }.withBigQuery().toListSuspending()
 *
 *     // Flow — 페이지 단위 스트리밍 (대용량 결과셋에 적합)
 *     Events.selectAll().withBigQuery().toFlow().collect { row -> ... }
 *
 *     // suspend DML
 *     Events.execInsertSuspending { it[eventId] = 1L; it[region] = "kr" }
 * }
 * ```
 *
 * @param bigquery BigQuery REST API 클라이언트
 * @param projectId BigQuery 프로젝트 ID
 * @param datasetId BigQuery 데이터셋 ID
 * @param sqlGenDb Exposed Statement → SQL 변환 전용 DB (PostgreSQL 모드 권장; [create] 팩토리로 자동 생성 가능)
 * @param dispatcher suspend 함수 실행 시 사용할 디스패처. 기본값은 [Dispatchers.IO].
 *   Virtual Thread 사용 시: `Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()`
 */
class BigQueryContext(
    val bigquery: Bigquery,
    val projectId: String,
    val datasetId: String,
    val sqlGenDb: Database,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object : KLogging() {
        /**
         * H2(PostgreSQL 모드) sqlGenDb를 자동 생성하는 팩토리.
         * 별도 Database 설정 없이 바로 사용 가능합니다.
         */
        fun create(
            bigquery: Bigquery,
            projectId: String,
            datasetId: String,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): BigQueryContext {
            val sqlGenDb = Database.connect(
                url = "jdbc:h2:mem:bq_sqlgen_${projectId}_${datasetId};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
            return BigQueryContext(bigquery, projectId, datasetId, sqlGenDb, dispatcher)
        }
    }

    // ── RAW SQL ───────────────────────────────────────────────────────────────

    /** 원시 SQL 문자열을 BigQuery에서 실행합니다. DML 또는 단순 조회에 사용합니다. */
    fun runRawQuery(sql: String): QueryResponse {
        val request = QueryRequest()
            .setQuery(sql.trimIndent().trim())
            .setUseLegacySql(false)
            .setDefaultDataset(DatasetReference().setProjectId(projectId).setDatasetId(datasetId))
            .setTimeoutMs(30_000L)

        return bigquery.jobs().query(projectId, request).execute()
            .also { it.checkErrors(sql) }
    }

    /** 원시 SQL 문자열을 BigQuery에서 비동기로 실행합니다. */
    suspend fun runRawQuerySuspending(sql: String): QueryResponse =
        withContext(dispatcher) { runRawQuery(sql) }

    // ── SELECT ────────────────────────────────────────────────────────────────

    /** Exposed [Query]를 SQL로 변환한 뒤 실행하고 [QueryResponse]를 반환합니다. */
    fun runQuery(query: Query): QueryResponse {
        val sql = transaction(sqlGenDb) { query.prepareSQL(this, prepared = false) }
        return runRawQuery(sql)
    }

    /**
     * Exposed [Query]를 [BigQueryQueryExecutor]로 래핑합니다.
     * [BigQueryQueryExecutor.toList]는 pageToken/jobComplete를 처리하여 전체 결과를 반환합니다.
     */
    fun Query.withBigQuery(): BigQueryQueryExecutor =
        BigQueryQueryExecutor(this, this@BigQueryContext)

    // ── INSERT ────────────────────────────────────────────────────────────────

    /**
     * Exposed INSERT DSL을 BigQuery에서 실행합니다.
     *
     * ```kotlin
     * with(context) {
     *     Events.execInsert { it[eventId] = 1L; it[region] = "kr" }
     * }
     * ```
     */
    fun <T : Table> T.execInsert(body: T.(InsertStatement<Number>) -> Unit): QueryResponse {
        val stmt = InsertStatement<Number>(this)
        body(stmt)
        val sql = transaction(sqlGenDb) { stmt.expandSql(this) }
        return runRawQuery(sql)
    }

    /** Exposed INSERT DSL을 BigQuery에서 비동기로 실행합니다. */
    suspend fun <T : Table> T.execInsertSuspending(body: T.(InsertStatement<Number>) -> Unit): QueryResponse =
        withContext(dispatcher) { execInsert(body) }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Exposed UPDATE DSL을 BigQuery에서 실행합니다.
     *
     * ```kotlin
     * with(context) {
     *     Events.execUpdate(Events.region eq "kr") { it[eventType] = "UPDATED" }
     * }
     * ```
     */
    fun <T : Table> T.execUpdate(
        where: Op<Boolean>,
        body: T.(UpdateStatement) -> Unit,
    ): QueryResponse {
        val stmt = UpdateStatement(this, limit = null, where = where)
        body(stmt)
        val sql = transaction(sqlGenDb) { stmt.expandSql(this) }
        return runRawQuery(sql)
    }

    /** Exposed UPDATE DSL을 BigQuery에서 비동기로 실행합니다. */
    suspend fun <T : Table> T.execUpdateSuspending(
        where: Op<Boolean>,
        body: T.(UpdateStatement) -> Unit,
    ): QueryResponse = withContext(dispatcher) { execUpdate(where, body) }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Exposed DELETE DSL을 BigQuery에서 실행합니다.
     *
     * ```kotlin
     * with(context) {
     *     Events.execDelete(Events.region eq "us")
     * }
     * ```
     */
    fun <T : Table> T.execDelete(where: Op<Boolean>): QueryResponse {
        val stmt = DeleteStatement(this, where = where)
        val sql = transaction(sqlGenDb) { stmt.expandSql(this) }
        return runRawQuery(sql)
    }

    /** Exposed DELETE DSL을 BigQuery에서 비동기로 실행합니다. */
    suspend fun <T : Table> T.execDeleteSuspending(where: Op<Boolean>): QueryResponse =
        withContext(dispatcher) { execDelete(where) }

    // ── DDL ──────────────────────────────────────────────────────────────────

    /**
     * Exposed [Table] 정의에서 CREATE TABLE DDL을 생성하여 BigQuery에서 실행합니다.
     *
     * [sqlGenDb](H2 PostgreSQL 모드)를 이용해 표준 SQL DDL을 생성한 뒤,
     * BigQuery(ZetaSQL) 호환 타입으로 변환하여 REST API로 전달합니다.
     *
     * 타입 매핑:
     * - `BIGINT` → `INT64`
     * - `VARCHAR(n)` → `STRING`
     * - `DECIMAL(p, s)` → `NUMERIC`
     * - standalone `NULL` 제거 (BigQuery는 nullable 컬럼에 NULL 키워드 불필요)
     *
     * 테이블이 이미 존재하면 에러가 발생하므로, 호출 전에 `DROP TABLE tableName` 을 실행하세요.
     */
    fun Table.execCreateTable() {
        transaction(sqlGenDb) { SchemaUtils.createStatements(this@execCreateTable) }
            .map { sql -> sql.toBigQueryDdl() }
            .forEach { runRawQuery(it) }
    }

    private fun String.toBigQueryDdl(): String = this
        .replace(Regex("\\bBIGINT\\b"), "INT64")
        .replace(Regex("\\bVARCHAR\\(\\d+\\)"), "STRING")
        .replace(Regex("\\bDECIMAL\\(\\d+,\\s*\\d+\\)"), "NUMERIC")
        .replace(Regex("(?<!NOT) NULL(?=[,)])"), "")

    /**
     * 테이블의 모든 행을 삭제합니다.
     *
     * BigQuery는 WHERE 절 없는 DELETE를 지원하지 않으므로 `WHERE TRUE`를 사용합니다.
     * [tableName]은 Exposed [Table] 객체의 상수값으로 SQL 인젝션 위험이 없습니다.
     */
    fun Table.execDeleteAll(): QueryResponse =
        runRawQuery("DELETE FROM $tableName WHERE TRUE")

    // ── INTERNAL ──────────────────────────────────────────────────────────────

    /**
     * SQL을 실행하고 pageToken/jobComplete를 처리하여 전체 행을 수집합니다.
     * [BigQueryQueryExecutor.toList]에서 내부적으로 사용합니다.
     */
    internal fun collectAllRows(sql: String): List<BigQueryResultRow> {
        val (schema, allRows) = fetchAllPages(sql)
        val fieldNames = schema?.fields?.map { it.name.lowercase() } ?: emptyList()
        return allRows.map { row ->
            val data = fieldNames.zip(row.f).associate { (name, cell) -> name to cell.v }
            BigQueryResultRow(data)
        }
    }

    /**
     * SQL을 실행하고 페이지 단위로 [BigQueryResultRow]를 emit하는 [Flow]를 반환합니다.
     * 대용량 결과셋을 메모리에 모두 올리지 않고 처리할 때 적합합니다.
     * [BigQueryQueryExecutor.toFlow]에서 내부적으로 사용합니다.
     */
    internal fun collectRowsFlow(sql: String): Flow<BigQueryResultRow> = flow {
        val request = QueryRequest()
            .setQuery(sql.trimIndent().trim())
            .setUseLegacySql(false)
            .setDefaultDataset(DatasetReference().setProjectId(projectId).setDatasetId(datasetId))
            .setTimeoutMs(30_000L)

        val initial = withContext(dispatcher) {
            bigquery.jobs().query(projectId, request).execute()
        }
        initial.checkErrors(sql)

        var schema = initial.schema
        val jobId = initial.jobReference?.jobId
        var pageToken = initial.pageToken
        var jobComplete = initial.jobComplete ?: true

        // 첫 페이지 emit
        val firstFieldNames = schema?.fields?.map { it.name.lowercase() } ?: emptyList()
        initial.rows?.forEach { row ->
            val data = firstFieldNames.zip(row.f).associate { (name, cell) -> name to cell.v }
            emit(BigQueryResultRow(data))
        }

        // 추가 페이지 emit
        while (!jobComplete || pageToken != null) {
            checkNotNull(jobId) { "jobReference가 없는 상태에서 추가 페이지를 요청할 수 없습니다." }
            val page = withContext(dispatcher) {
                bigquery.jobs().getQueryResults(projectId, jobId)
                    .apply { if (pageToken != null) setPageToken(pageToken) }
                    .setTimeoutMs(30_000L)
                    .execute()
            }

            page.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
                val msg = errors.joinToString("; ") { it.message ?: it.reason ?: "unknown" }
                throw RuntimeException("BigQuery 쿼리 오류: $msg")
            }

            if (schema == null) schema = page.schema
            val fieldNames = schema?.fields?.map { it.name.lowercase() } ?: emptyList()
            page.rows?.forEach { row ->
                val data = fieldNames.zip(row.f).associate { (name, cell) -> name to cell.v }
                emit(BigQueryResultRow(data))
            }
            pageToken = page.pageToken
            jobComplete = page.jobComplete ?: true
        }
    }

    private fun fetchAllPages(sql: String): Pair<com.google.api.services.bigquery.model.TableSchema?, List<TableRow>> {
        val request = QueryRequest()
            .setQuery(sql.trimIndent().trim())
            .setUseLegacySql(false)
            .setDefaultDataset(DatasetReference().setProjectId(projectId).setDatasetId(datasetId))
            .setTimeoutMs(30_000L)

        val initial = bigquery.jobs().query(projectId, request).execute()
        initial.checkErrors(sql)

        val jobId = initial.jobReference?.jobId
        val allRows = mutableListOf<TableRow>()
        allRows.addAll(initial.rows ?: emptyList())

        var schema = initial.schema
        var pageToken = initial.pageToken
        var jobComplete = initial.jobComplete ?: true

        while (!jobComplete || pageToken != null) {
            checkNotNull(jobId) { "jobReference가 없는 상태에서 추가 페이지를 요청할 수 없습니다." }
            val page = bigquery.jobs().getQueryResults(projectId, jobId)
                .apply { if (pageToken != null) setPageToken(pageToken) }
                .setTimeoutMs(30_000L)
                .execute()

            page.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
                val msg = errors.joinToString("; ") { it.message ?: it.reason ?: "unknown" }
                throw RuntimeException("BigQuery 쿼리 오류: $msg")
            }

            if (schema == null) schema = page.schema
            allRows.addAll(page.rows ?: emptyList())
            pageToken = page.pageToken
            jobComplete = page.jobComplete ?: true
        }

        return schema to allRows
    }

    private fun QueryResponse.checkErrors(sql: String) {
        if (errors?.isNotEmpty() == true) {
            val msg = errors.joinToString("; ") { it.message ?: it.reason ?: "unknown" }
            throw RuntimeException("BigQuery 쿼리 오류: $msg\nSQL: ${sql.take(200)}")
        }
    }

    private fun Statement<*>.expandSql(transaction: org.jetbrains.exposed.v1.core.Transaction): String {
        val firstArgs = arguments().firstOrNull()
        return if (firstArgs == null || !firstArgs.iterator().hasNext()) {
            prepareSQL(transaction, prepared = false)
        } else {
            StatementContext(this, firstArgs).expandArgs(transaction)
        }
    }
}
