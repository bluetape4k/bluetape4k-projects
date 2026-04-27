package io.bluetape4k.exposed.bigquery

import com.google.api.services.bigquery.Bigquery
import com.google.api.services.bigquery.model.DatasetReference
import com.google.api.services.bigquery.model.QueryRequest
import com.google.api.services.bigquery.model.QueryResponse
import com.google.api.services.bigquery.model.TableRow
import io.bluetape4k.exposed.bigquery.BigQueryContext.Companion.create
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
import java.util.*

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
 * - **분리**: JDBC 트랜잭션 일관성이나 Trino connector 기반 실행이 필요하면 `bluetape4k-exposed-trino` 또는
 *   후속 `exposed-bigquery-trino` 모듈을 사용해야 합니다.
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
    companion object: KLogging() {
        private const val DEFAULT_QUERY_TIMEOUT_MS = 30_000L

        private val DB_NAME_SANITIZE_REGEX = Regex("[^A-Za-z0-9_]")
        private val BIGINT_REGEX = Regex("\\bBIGINT\\b")
        private val VARCHAR_REGEX = Regex("\\bVARCHAR\\(\\d+\\)")
        private val DECIMAL_REGEX = Regex("\\bDECIMAL\\(\\d+,\\s*\\d+\\)")
        private val STANDALONE_NULL_REGEX = Regex("(?<!NOT) NULL(?=[,)])")

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
            val dbName = "bq_sqlgen_${projectId}_${datasetId}"
                .replace(DB_NAME_SANITIZE_REGEX, "_")
            val sqlGenDb = Database.connect(
                url = "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
            return BigQueryContext(bigquery, projectId, datasetId, sqlGenDb, dispatcher)
        }
    }

    /**
     * BigQuery REST API 요청 객체를 생성합니다.
     *
     * Legacy SQL은 사용하지 않으며(ZetaSQL 표준), 데이터셋 컨텍스트와 쿼리 타임아웃을 설정합니다.
     * 앞뒤 공백을 제거하여 BigQuery 파서가 불필요한 공백으로 오류를 반환하는 상황을 방지합니다.
     */
    private fun newQueryRequest(sql: String): QueryRequest =
        QueryRequest()
            .setQuery(sql.trimIndent().trim())
            .setUseLegacySql(false)
            .setDefaultDataset(DatasetReference().setProjectId(projectId).setDatasetId(datasetId))
            .setTimeoutMs(DEFAULT_QUERY_TIMEOUT_MS)

    // ── RAW SQL ───────────────────────────────────────────────────────────────

    /**
     * 원시 SQL 문자열을 BigQuery에서 실행합니다. DML 또는 단순 조회에 사용합니다.
     *
     * 서버가 오류를 반환하면 [BigQueryQueryException]을 던집니다.
     *
     * @param sql 실행할 SQL 문자열 (표준 SQL, Legacy SQL 불가)
     * @throws BigQueryQueryException BigQuery 서버 오류 응답 시
     */
    fun runRawQuery(sql: String): QueryResponse {
        return bigquery.jobs().query(projectId, newQueryRequest(sql)).execute()
            .also { it.checkErrors(sql) }
    }

    /**
     * 원시 SQL 문자열을 BigQuery에서 비동기로 실행합니다.
     *
     * [dispatcher]에서 블로킹 HTTP 호출을 수행하므로 코루틴에서 안전하게 호출할 수 있습니다.
     *
     * @param sql 실행할 SQL 문자열
     * @throws BigQueryQueryException BigQuery 서버 오류 응답 시
     */
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
    fun <T: Table> T.execInsert(body: T.(InsertStatement<Number>) -> Unit): QueryResponse {
        val stmt = InsertStatement<Number>(this)
        body(stmt)
        val sql = transaction(sqlGenDb) { stmt.expandSql(this) }
        return runRawQuery(sql)
    }

    /** Exposed INSERT DSL을 BigQuery에서 비동기로 실행합니다. */
    suspend fun <T: Table> T.execInsertSuspending(body: T.(InsertStatement<Number>) -> Unit): QueryResponse =
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
    fun <T: Table> T.execUpdate(
        where: Op<Boolean>,
        body: T.(UpdateStatement) -> Unit,
    ): QueryResponse {
        val stmt = UpdateStatement(this, limit = null, where = where)
        body(stmt)
        val sql = transaction(sqlGenDb) { stmt.expandSql(this) }
        return runRawQuery(sql)
    }

    /** Exposed UPDATE DSL을 BigQuery에서 비동기로 실행합니다. */
    suspend fun <T: Table> T.execUpdateSuspending(
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
    fun <T: Table> T.execDelete(where: Op<Boolean>): QueryResponse {
        val stmt = DeleteStatement(this, where = where)
        val sql = transaction(sqlGenDb) { stmt.expandSql(this) }
        return runRawQuery(sql)
    }

    /** Exposed DELETE DSL을 BigQuery에서 비동기로 실행합니다. */
    suspend fun <T: Table> T.execDeleteSuspending(where: Op<Boolean>): QueryResponse =
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
        .replace(BIGINT_REGEX, "INT64")
        .replace(VARCHAR_REGEX, "STRING")
        .replace(DECIMAL_REGEX, "NUMERIC")
        .replace(STANDALONE_NULL_REGEX, "")

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
        val fieldNames = schema?.fields?.map { it.name.lowercase(Locale.ROOT) } ?: emptyList()
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
        val initial = withContext(dispatcher) {
            bigquery.jobs().query(projectId, newQueryRequest(sql)).execute()
        }
        initial.checkErrors(sql)

        var schema = initial.schema
        val jobId = initial.jobReference?.jobId
        var pageToken = initial.pageToken
        var jobComplete = initial.jobComplete ?: true

        // 첫 페이지 emit
        val firstFieldNames = schema?.fields?.map { it.name.lowercase(Locale.ROOT) } ?: emptyList()
        initial.rows?.forEach { row ->
            val data = firstFieldNames.zip(row.f).associate { (name, cell) -> name to cell.v }
            emit(BigQueryResultRow(data))
        }

        // 추가 페이지 emit
        while (!jobComplete || pageToken != null) {
            // 취소 시그널을 다음 HTTP 요청 직전에 확인한다.
            // withContext(dispatcher) 내부에서만 취소를 감지하면 한 페이지 전체를 내려받은 뒤에야
            // 취소가 전파되므로, 루프 선두에서 즉시 확인하여 불필요한 네트워크 비용을 막는다.
            currentCoroutineContext().ensureActive()
            checkNotNull(jobId) { "jobReference가 없는 상태에서 추가 페이지를 요청할 수 없습니다." }
            val page = withContext(dispatcher) {
                bigquery.jobs().getQueryResults(projectId, jobId)
                    .apply { if (pageToken != null) setPageToken(pageToken) }
                    .setTimeoutMs(DEFAULT_QUERY_TIMEOUT_MS)
                    .execute()
            }

            // 추가 페이지 응답에도 errors 필드가 포함될 수 있다.
            // RuntimeException 대신 BigQueryQueryException을 던져 호출자가 BigQuery 오류임을 명확히 구분하게 한다.
            page.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
                val msg = errors.joinToString("; ") { it.message ?: it.reason ?: "unknown" }
                throw BigQueryQueryException("BigQuery 쿼리 오류: $msg")
            }

            if (schema == null) schema = page.schema
            val fieldNames = schema?.fields?.map { it.name.lowercase(Locale.ROOT) } ?: emptyList()
            page.rows?.forEach { row ->
                val data = fieldNames.zip(row.f).associate { (name, cell) -> name to cell.v }
                emit(BigQueryResultRow(data))
            }
            pageToken = page.pageToken
            jobComplete = page.jobComplete ?: true
        }
    }

    private fun fetchAllPages(sql: String): Pair<com.google.api.services.bigquery.model.TableSchema?, List<TableRow>> {
        val initial = bigquery.jobs().query(projectId, newQueryRequest(sql)).execute()
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
                .setTimeoutMs(DEFAULT_QUERY_TIMEOUT_MS)
                .execute()

            // collectAllRows(동기 버전)에서도 페이지 단위 오류를 동일하게 처리한다.
            // RuntimeException 대신 BigQueryQueryException으로 던져 호출자가 일관성 있게 catch할 수 있게 한다.
            page.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
                val msg = errors.joinToString("; ") { it.message ?: it.reason ?: "unknown" }
                throw BigQueryQueryException("BigQuery 쿼리 오류: $msg")
            }

            if (schema == null) schema = page.schema
            allRows.addAll(page.rows ?: emptyList())
            pageToken = page.pageToken
            jobComplete = page.jobComplete ?: true
        }

        return schema to allRows
    }

    // QueryResponse.errors 는 최초 쿼리 응답(runRawQuery)에서 발생한 오류를 담는다.
    // SQL을 최대 200자로 잘라 메시지에 포함하는 이유: BigQuery 오류 메시지만으로는 어떤 SQL이 실패했는지
    // 파악하기 어렵기 때문이다. 200자 제한은 로그 라인 길이를 적정 수준으로 유지한다.
    private fun QueryResponse.checkErrors(sql: String) {
        if (errors?.isNotEmpty() == true) {
            val msg = errors.joinToString("; ") { it.message ?: it.reason ?: "unknown" }
            throw BigQueryQueryException("BigQuery 쿼리 오류: $msg\nSQL: ${sql.take(200)}")
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
