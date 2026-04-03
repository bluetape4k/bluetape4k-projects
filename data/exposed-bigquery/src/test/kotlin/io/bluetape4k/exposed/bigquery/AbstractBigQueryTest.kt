package io.bluetape4k.exposed.bigquery

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.bigquery.Bigquery
import com.google.api.services.bigquery.model.QueryResponse
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import io.bluetape4k.exposed.bigquery.AbstractBigQueryTest.Companion.setupEventsTable
import io.bluetape4k.exposed.bigquery.domain.Events
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.junit.jupiter.api.BeforeAll

/**
 * BigQuery 에뮬레이터(goccy/bigquery-emulator)를 사용하는 테스트 기반 클래스.
 *
 * [BigQueryContext]를 통해 Exposed DSL과 유사한 방식으로 쿼리를 실행합니다.
 * - 로컬 에뮬레이터 실행 중 (localhost:9050): 자동 감지하여 사용
 * - 미실행 시: Testcontainers Docker 컨테이너 자동 시작
 *
 * ## 사전 조건
 *
 * ```bash
 * brew install goccy/bigquery-emulator/bigquery-emulator
 * bigquery-emulator --project=test --dataset=testdb --port=9050
 * ```
 *
 * ## 사용 예
 *
 * ```kotlin
 * // SELECT
 * val rows = Events.selectAll().where { Events.region eq "kr" }.withBigQuery().toList()
 * val region: String = rows[0][Events.region]
 *
 * // INSERT
 * Events.execInsert { it[eventId] = 1L; it[region] = "kr" }
 *
 * // UPDATE / DELETE
 * Events.execUpdate(Events.region eq "kr") { it[eventType] = "UPDATED" }
 * Events.execDelete(Events.region eq "us")
 *
 * // 원시 SQL
 * runRawQuery("SELECT COUNT(*) FROM events")
 * ```
 */
abstract class AbstractBigQueryTest {

    companion object : KLogging() {

        /** H2(PostgreSQL 모드) SQL 생성 전용 연결. Exposed Statement → SQL 변환에 사용합니다. */
        private val sqlGenDb: Database by lazy {
            Database.connect(
                url = "jdbc:h2:mem:sqlgen;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
        }

        /** BigQuery REST API 클라이언트. 에뮬레이터 HTTP 엔드포인트를 setRootUrl 로 지정합니다. */
        private val bigqueryClient: Bigquery by lazy {
            val transport = GoogleNetHttpTransport.newTrustedTransport()
            val json = GsonFactory.getDefaultInstance()
            val credentials = GoogleCredentials.create(AccessToken("emulator-fake-token", null))
            val requestInitializer = HttpCredentialsAdapter(credentials)
            Bigquery.Builder(transport, json, requestInitializer)
                .setRootUrl("http://${BigQueryEmulator.host}:${BigQueryEmulator.port}/")
                .setApplicationName("exposed-bigquery-test")
                .build()
        }

        /** BigQuery 실행 컨텍스트. [BigQueryContext]의 DSL 함수를 직접 사용하려면 `with(bqContext) { }` 블록을 사용합니다. */
        val bqContext: BigQueryContext by lazy {
            BigQueryContext(
                bigquery = bigqueryClient,
                projectId = BigQueryEmulator.PROJECT_ID,
                datasetId = BigQueryEmulator.DATASET,
                sqlGenDb = sqlGenDb,
            )
        }

        /** 원시 SQL 문자열을 에뮬레이터에서 실행합니다. */
        fun runRawQuery(sql: String): QueryResponse = bqContext.runRawQuery(sql)

        /** Exposed [Query]를 SQL로 변환한 뒤 에뮬레이터에서 실행합니다. */
        fun runQuery(query: Query): QueryResponse = bqContext.runQuery(query)

        /**
         * 테스트 클래스별로 한 번만 실행됩니다.
         *
         * Exposed [Events] 테이블 정의에서 DDL을 생성해 에뮬레이터에 적용합니다.
         * raw SQL 대신 [BigQueryContext.execCreateTable]을 사용하므로 컬럼 추가 시 자동으로 반영됩니다.
         */
        @JvmStatic
        @BeforeAll
        fun setupEventsTable() {
            // 이전 테스트 실행에서 남은 테이블 정리 (에뮬레이터는 DROP TABLE IF EXISTS 미지원)
            runCatching { runRawQuery("DROP TABLE ${Events.tableName}") }
            // Exposed Table 정의에서 DDL 자동 생성 후 실행
            with(bqContext) { Events.execCreateTable() }
        }
    }

    // ── SELECT ────────────────────────────────────────────────────────────────

    protected fun Query.withBigQuery(): BigQueryQueryExecutor {
        val q = this
        return with(bqContext) { q.withBigQuery() }
    }

    // ── INSERT ────────────────────────────────────────────────────────────────

    protected fun <T : Table> T.execInsert(body: T.(InsertStatement<Number>) -> Unit): QueryResponse {
        val t = this
        return with(bqContext) { t.execInsert(body) }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    protected fun <T : Table> T.execUpdate(
        where: Op<Boolean>,
        body: T.(UpdateStatement) -> Unit,
    ): QueryResponse {
        val t = this
        return with(bqContext) { t.execUpdate(where, body) }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    protected fun <T : Table> T.execDelete(where: Op<Boolean>): QueryResponse {
        val t = this
        return with(bqContext) { t.execDelete(where) }
    }

    // ── TEST DATA LIFECYCLE ──────────────────────────────────────────────────

    /**
     * 테스트 전에 events 테이블 데이터를 초기화하고 블록을 실행합니다.
     *
     * 테이블 자체는 [setupEventsTable]에서 테스트 클래스별로 한 번만 생성됩니다.
     * 매 테스트마다 DROP/CREATE 하지 않고 데이터만 삭제하여 테스트 속도를 개선합니다.
     */
    protected fun withEventsData(block: () -> Unit) {
        with(bqContext) { Events.execDeleteAll() }
        block()
    }

    /**
     * suspend 테스트 전에 events 테이블 데이터를 초기화하고 suspend 블록을 실행합니다.
     *
     * suspend API 회귀 테스트에서 `runTest`와 함께 사용합니다.
     */
    protected suspend fun withEventsDataSuspending(block: suspend () -> Unit) {
        with(bqContext) { Events.execDeleteAll() }
        block()
    }
}
