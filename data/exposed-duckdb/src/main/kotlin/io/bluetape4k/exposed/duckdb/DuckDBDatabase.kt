package io.bluetape4k.exposed.duckdb

import io.bluetape4k.exposed.duckdb.DuckDBDatabase.file
import io.bluetape4k.exposed.duckdb.dialect.DuckDBDialect
import io.bluetape4k.exposed.duckdb.dialect.DuckDBDialectMetadata
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.v1.core.DatabaseApi
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.DriverManager

/**
 * DuckDB 데이터베이스 연결 팩토리.
 *
 * ## 인메모리 사용 예
 *
 * ```kotlin
 * val db = DuckDBDatabase.inMemory()
 * transaction(db) {
 *     SchemaUtils.create(Events)
 *     Events.insert { it[eventId] = 1L; it[region] = "kr" }
 *     val rows = Events.selectAll().toList()
 * }
 * ```
 *
 * ## 파일 기반 사용 예
 *
 * ```kotlin
 * val db = DuckDBDatabase.file("/tmp/analytics.db")
 * transaction(db) {
 *     SchemaUtils.create(Events)
 * }
 * ```
 *
 * ## 코루틴 사용 예
 *
 * ```kotlin
 * val db = DuckDBDatabase.file("/tmp/analytics.db")
 * val rows = suspendTransaction(db) {
 *     Events.selectAll().toList()
 * }
 *
 * queryFlow(db) {
 *     Events.selectAll()
 * }.collect { row -> ... }
 * ```
 *
 * ## 인메모리 연결 주의사항
 *
 * `DuckDBDatabase.inMemory()` 는 새 연결마다 독립된 인메모리 DB를 생성합니다.
 * 여러 트랜잭션이 같은 인메모리 상태를 공유해야 하면 파일 기반 DB를 사용하거나,
 * 테스트처럼 `DuckDBConnection.duplicate()` 기반 공유 연결을 사용해야 합니다.
 */
object DuckDBDatabase: KLogging() {

    /**
     * DuckDB JDBC 드라이버 클래스명.
     * `const val` 대신 `val`을 사용하여 이 프로퍼티 접근 시 객체 초기화(init{})를 보장합니다.
     */
    val DRIVER = "org.duckdb.DuckDBDriver"

    init {
        // Database.Companion.init{}를 먼저 트리거한 뒤 DuckDB 드라이버/다이얼렉트 등록
        Database.registerJdbcDriver("jdbc:duckdb", DRIVER, DuckDBDialect.dialectName)
        DatabaseApi.registerDialect(DuckDBDialect.dialectName) { DuckDBDialect() }
        Database.registerDialectMetadata(DuckDBDialect.dialectName) { DuckDBDialectMetadata() }
        log.debug("DuckDB dialect registered: ${DuckDBDialect.dialectName}")
    }

    /**
     * 인메모리 DuckDB 데이터베이스 연결.
     *
     * **주의**: 연결별로 독립된 인메모리 DB가 생성됩니다.
     * 테스트에서 데이터를 여러 트랜잭션에 걸쳐 유지하려면 [file]을 사용하거나
     * HikariCP에서 `maximumPoolSize=1`로 설정하세요.
     */
    fun inMemory(): Database = Database.connect(
        getNewConnection = { DuckDBConnectionWrapper(DriverManager.getConnection("jdbc:duckdb:")) }
    )

    /**
     * 파일 기반 DuckDB 데이터베이스 연결.
     * 파일이 없으면 새로 생성합니다.
     *
     * @param path 데이터베이스 파일 경로 (예: `/tmp/analytics.db`)
     */
    fun file(path: String): Database =
        connectJdbc("jdbc:duckdb:${path.requireNotBlank("path")}")

    /**
     * 읽기 전용 파일 기반 DuckDB 데이터베이스 연결.
     *
     * @param path 데이터베이스 파일 경로
     */
    fun readOnly(path: String): Database =
        connectJdbc("jdbc:duckdb:${path.requireNotBlank("path")}?access_mode=read_only")

    private fun connectJdbc(url: String): Database = Database.connect(
        getNewConnection = { DuckDBConnectionWrapper(DriverManager.getConnection(url)) }
    )
}
