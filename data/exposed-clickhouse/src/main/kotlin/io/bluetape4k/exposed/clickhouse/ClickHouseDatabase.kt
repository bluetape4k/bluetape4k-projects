package io.bluetape4k.exposed.clickhouse

import io.bluetape4k.exposed.clickhouse.dialect.ClickHouseDialect
import io.bluetape4k.exposed.clickhouse.dialect.ClickHouseDialectMetadata
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.DatabaseApi
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.DriverManager
import java.util.Properties

/**
 * ClickHouse 데이터베이스 연결 팩토리.
 *
 * ClickHouse JDBC 드라이버를 통해 Exposed ORM과 연동할 수 있도록
 * 드라이버/다이얼렉트 등록 및 연결 생성을 담당합니다.
 *
 * ## 기본 사용 예
 *
 * ```kotlin
 * val db = ClickHouseDatabase.connect(
 *     host = "clickhouse-host",
 *     port = 8123,
 *     database = "default",
 *     user = "test",
 *     password = "test",
 * )
 * transaction(db) {
 *     val rows = MyTable.selectAll().toList()
 * }
 * ```
 *
 * ## JDBC URL 직접 사용 예
 *
 * ```kotlin
 * val db = ClickHouseDatabase.connect(
 *     jdbcUrl = "jdbc:clickhouse://host:8123/default",
 *     user = "test",
 *     password = "test",
 * )
 * ```
 *
 * ## autocommit 주의사항
 *
 * - ClickHouse는 트랜잭션을 지원하지 않습니다. 모든 문(statement)은 autocommit 모드로 실행됩니다.
 * - `transaction {}` 블록 내 다중 DML 실행 시, 중간 실패가 발생하면 앞선 DML은 롤백되지 않습니다.
 * - `rollback()`은 no-op입니다 — Exposed 프레임워크 호환을 위한 어댑터입니다.
 * - Nested transaction / Savepoint는 미지원됩니다 — 호출은 허용되나 원자성이 보장되지 않습니다.
 */
object ClickHouseDatabase: KLogging() {

    /**
     * ClickHouse JDBC 드라이버 클래스명.
     *
     * `const val` 대신 `val`을 사용하여 이 프로퍼티 접근 시 객체 초기화(init{})를 보장합니다.
     * `const val`은 컴파일 타임에 인라인되므로 객체 초기화를 트리거하지 않을 수 있습니다.
     */
    val DRIVER = "com.clickhouse.jdbc.ClickHouseDriver"

    init {
        Database.registerJdbcDriver("jdbc:clickhouse", DRIVER, ClickHouseDialect.dialectName)
        DatabaseApi.registerDialect(ClickHouseDialect.dialectName) { ClickHouseDialect() }
        Database.registerDialectMetadata(ClickHouseDialect.dialectName) { ClickHouseDialectMetadata() }
        log.debug("ClickHouse dialect registered: ${ClickHouseDialect.dialectName}")
    }

    /**
     * ClickHouse 데이터베이스에 연결합니다.
     *
     * JDBC URL을 `jdbc:clickhouse://{host}:{port}/{database}` 형식으로 조합합니다.
     *
     * **주의**: ClickHouse는 트랜잭션을 지원하지 않습니다. autocommit 모드로 실행되며,
     * 블록 중간 실패 시 앞선 DML은 롤백되지 않습니다.
     *
     * @param host ClickHouse 호스트 (기본값: `localhost`)
     * @param port ClickHouse HTTP 포트 (기본값: `8123`)
     * @param database ClickHouse 데이터베이스 이름 (기본값: `default`)
     * @param user 접속 사용자 (기본값: `default`)
     * @param password 접속 비밀번호 (기본값: 빈 문자열)
     * @return Exposed [Database] 인스턴스
     */
    fun connect(
        host: String = "localhost",
        port: Int = 8123,
        database: String = "default",
        user: String = "default",
        password: String = "",
    ): Database {
        // 빈 값으로 JDBC URL을 구성하면 무효한 URL이 만들어져 DriverManager.getConnection()
        // 호출 시점에 불명확한 예외가 발생합니다. 조기에 명확한 메시지로 실패시킵니다.
        requireNotNull(host.ifBlank { null }) { "host는 공백일 수 없습니다." }
        // 유효하지 않은 포트 번호는 TCP 연결 시도 단계에서야 실패하므로, 미리 차단합니다.
        require(port in 1..65535) { "port는 1~65535 범위여야 합니다: $port" }
        // ClickHouse JDBC URL은 `database`를 path 세그먼트로 요구합니다.
        requireNotNull(database.ifBlank { null }) { "database는 공백일 수 없습니다." }

        val url = "jdbc:clickhouse://$host:$port/$database"
        return Database.connect(
            getNewConnection = {
                val props = Properties().apply {
                    setProperty("user", user)
                    setProperty("password", password)
                }
                // 연결 획득 후 래퍼 생성 실패 시 원본 연결을 닫아 leak을 방지합니다.
                val raw = DriverManager.getConnection(url, props)
                runCatching { ClickHouseConnectionWrapper(raw) }
                    .getOrElse { e -> raw.runCatching { close() }; throw e }
            }
        )
    }

    /**
     * JDBC URL을 직접 지정하여 ClickHouse 데이터베이스에 연결합니다.
     *
     * **주의**: ClickHouse는 트랜잭션을 지원하지 않습니다. autocommit 모드로 실행되며,
     * 블록 중간 실패 시 앞선 DML은 롤백되지 않습니다.
     *
     * @param jdbcUrl ClickHouse JDBC URL (예: `jdbc:clickhouse://host:8123/default`)
     * @param user 접속 사용자 (기본값: `default`)
     * @param password 접속 비밀번호 (기본값: 빈 문자열)
     * @return Exposed [Database] 인스턴스
     */
    fun connect(
        jdbcUrl: String,
        user: String = "default",
        password: String = "",
    ): Database {
        // 빈 URL은 DriverManager.getConnection()에서 No suitable driver 예외를 발생시킵니다.
        requireNotNull(jdbcUrl.ifBlank { null }) { "jdbcUrl은 공백일 수 없습니다." }
        // ClickHouse 드라이버는 "jdbc:clickhouse://" 접두사가 있는 URL만 처리합니다.
        require(jdbcUrl.startsWith("jdbc:clickhouse://")) {
            "jdbcUrl은 'jdbc:clickhouse://'로 시작해야 합니다: $jdbcUrl"
        }

        return Database.connect(
            getNewConnection = {
                val props = Properties().apply {
                    setProperty("user", user)
                    setProperty("password", password)
                }
                // 연결 획득 후 래퍼 생성 실패 시 원본 연결을 닫아 leak을 방지합니다.
                val raw = DriverManager.getConnection(jdbcUrl, props)
                runCatching { ClickHouseConnectionWrapper(raw) }
                    .getOrElse { e -> raw.runCatching { close() }; throw e }
            }
        )
    }
}
