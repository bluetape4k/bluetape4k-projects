package io.bluetape4k.exposed.trino

import io.bluetape4k.exposed.trino.dialect.TrinoDialect
import io.bluetape4k.exposed.trino.dialect.TrinoDialectMetadata
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.DatabaseApi
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.DriverManager
import java.util.Properties

/**
 * Trino 데이터베이스 연결 팩토리.
 *
 * Trino JDBC 드라이버를 통해 Exposed ORM과 연동할 수 있도록
 * 드라이버/다이얼렉트 등록 및 연결 생성을 담당합니다.
 *
 * ## 기본 사용 예
 *
 * ```kotlin
 * val db = TrinoDatabase.connect(
 *     host = "trino-coordinator",
 *     port = 8080,
 *     catalog = "hive",
 *     schema = "default",
 *     user = "analyst",
 * )
 * transaction(db) {
 *     val rows = MyTable.selectAll().toList()
 * }
 * ```
 *
 * ## 코루틴 사용 예
 *
 * ```kotlin
 * val db = TrinoDatabase.connect("jdbc:trino://host:8080/hive/default", user = "analyst")
 *
 * val rows = suspendTransaction(db) {
 *     MyTable.selectAll().toList()
 * }
 *
 * queryFlow(db) {
 *     MyTable.selectAll()
 * }.collect { row -> ... }
 * ```
 *
 * ## autocommit 주의사항
 *
 * - Trino는 트랜잭션을 지원하지 않습니다. 모든 문(statement)은 autocommit 모드로 실행됩니다.
 * - `transaction {}` 블록 내 다중 DML 실행 시, 중간 실패가 발생하면 앞선 DML은 롤백되지 않습니다.
 * - `rollback()`은 no-op입니다 — Exposed 프레임워크 호환을 위한 어댑터입니다.
 * - Nested transaction / Savepoint는 미지원됩니다 — 호출은 허용되나 원자성이 보장되지 않습니다.
 */
object TrinoDatabase: KLogging() {

    /**
     * Trino JDBC 드라이버 클래스명.
     *
     * `const val` 대신 `val`을 사용하여 이 프로퍼티 접근 시 객체 초기화(init{})를 보장합니다.
     * `const val`은 컴파일 타임에 인라인되므로 객체 초기화를 트리거하지 않을 수 있습니다.
     */
    val DRIVER = "io.trino.jdbc.TrinoDriver"

    init {
        Database.registerJdbcDriver("jdbc:trino", DRIVER, TrinoDialect.dialectName)
        DatabaseApi.registerDialect(TrinoDialect.dialectName) { TrinoDialect() }
        Database.registerDialectMetadata(TrinoDialect.dialectName) { TrinoDialectMetadata() }
        log.debug("Trino dialect registered: ${TrinoDialect.dialectName}")
    }

    /**
     * Trino 데이터베이스에 연결합니다.
     *
     * JDBC URL을 `jdbc:trino://{host}:{port}/{catalog}/{schema}` 형식으로 조합합니다.
     *
     * **주의**: Trino는 트랜잭션을 지원하지 않습니다. autocommit 모드로 실행되며,
     * 블록 중간 실패 시 앞선 DML은 롤백되지 않습니다.
     *
     * @param host Trino 코디네이터 호스트 (기본값: `localhost`)
     * @param port Trino 코디네이터 포트 (기본값: `8080`)
     * @param catalog Trino 카탈로그 이름 (기본값: `memory`)
     * @param schema Trino 스키마 이름 (기본값: `default`)
     * @param user 접속 사용자 (기본값: `trino`)
     * @return Exposed [Database] 인스턴스
     */
    fun connect(
        host: String = "localhost",
        port: Int = 8080,
        catalog: String = "memory",
        schema: String = "default",
        user: String = "trino",
    ): Database {
        val url = "jdbc:trino://$host:$port/$catalog/$schema"
        return Database.connect(
            getNewConnection = {
                val props = Properties().apply { setProperty("user", user) }
                TrinoConnectionWrapper(DriverManager.getConnection(url, props))
            }
        )
    }

    /**
     * JDBC URL을 직접 지정하여 Trino 데이터베이스에 연결합니다.
     *
     * **주의**: Trino는 트랜잭션을 지원하지 않습니다. autocommit 모드로 실행되며,
     * 블록 중간 실패 시 앞선 DML은 롤백되지 않습니다.
     *
     * @param jdbcUrl Trino JDBC URL (예: `jdbc:trino://host:8080/hive/default`)
     * @param user 접속 사용자 (기본값: `trino`)
     * @return Exposed [Database] 인스턴스
     */
    fun connect(
        jdbcUrl: String,
        user: String = "trino",
    ): Database {
        return Database.connect(
            getNewConnection = {
                val props = Properties().apply { setProperty("user", user) }
                TrinoConnectionWrapper(DriverManager.getConnection(jdbcUrl, props))
            }
        )
    }

    // Phase 2: DataSource 기반 연결
    // fun connect(dataSource: javax.sql.DataSource): Database { ... }
}
