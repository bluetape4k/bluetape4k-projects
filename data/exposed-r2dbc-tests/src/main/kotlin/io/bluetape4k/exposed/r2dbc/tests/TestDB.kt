package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.exposed.r2dbc.tests.TestDBConfig.useFastDB
import io.bluetape4k.exposed.r2dbc.tests.TestDBConfig.useTestcontainers
import io.bluetape4k.logging.KLogging
import io.r2dbc.spi.IsolationLevel
import org.h2.engine.Mode
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties

/**
 * Exposed R2DBC 기능을 테스트하기 위한 대상 DB 목록 및 연결 정보를 제공하는 열거형입니다.
 *
 * 각 항목은 R2DBC 연결 문자열, 드라이버 클래스명, 인증 정보, 연결 전후 훅,
 * [R2dbcDatabaseConfig.Builder] 설정 람다를 포함합니다.
 *
 * 활성화된 dialect 목록은 [TestDBConfig.useFastDB] 설정에 따라 달라집니다:
 * - `useFastDB = true` (기본값): H2 메모리 DB만 사용 (빠른 로컬 테스트)
 * - `useFastDB = false`: H2, PostgreSQL, MySQL V8 사용 (Testcontainers 필요)
 *
 * ## 지원 DB 목록
 * | TestDB 항목     | R2DBC Driver       | 비고                          |
 * |---------------|--------------------|-------------------------------|
 * | H2            | r2dbc-h2           | 메모리 DB, 기본 격리 수준 READ_COMMITTED |
 * | H2_MYSQL      | r2dbc-h2           | MySQL 호환 모드                  |
 * | H2_MARIADB    | r2dbc-h2           | MariaDB 호환 모드                |
 * | H2_PSQL       | r2dbc-h2           | PostgreSQL 호환 모드             |
 * | MARIADB       | r2dbc-mariadb      | Testcontainers 또는 로컬 서버      |
 * | MYSQL_V5      | r2dbc-mysql        | R2DBC 드라이버 호환성 문제로 비활성화     |
 * | MYSQL_V8      | r2dbc-mysql        | Testcontainers 또는 로컬 서버      |
 * | POSTGRESQL    | r2dbc-postgresql   | Testcontainers 또는 로컬 서버      |
 */
enum class TestDB(
    val connection: () -> String,
    val driver: String,
    val user: String = "test",
    val pass: String = "test",
    val beforeConnection: suspend () -> Unit = {},
    // val afterConnection: (connection: Connection) -> Unit = {},
    val afterTestFinished: () -> Unit = {},
    val dbConfig: R2dbcDatabaseConfig.Builder.() -> Unit = {},
) {
    /**
     * H2 v2+ 인메모리 데이터베이스 (기본 격리 수준: READ_COMMITTED).
     *
     * R2DBC 연결 문자열: `r2dbc:h2:mem:///regular;DB_CLOSE_DELAY=-1;`
     */
    H2(
        connection = { "r2dbc:h2:mem:///regular;DB_CLOSE_DELAY=-1;" },
        driver = "org.h2.Driver",
        dbConfig = {
            defaultR2dbcIsolationLevel = IsolationLevel.READ_COMMITTED
        }
    ),

    /**
     * MySQL 호환 모드로 실행되는 H2 인메모리 데이터베이스.
     *
     * `convertInsertNullToZero` 속성을 `false`로 설정하여 NULL 삽입 시 0 변환을 방지합니다.
     *
     * R2DBC 연결 문자열: `r2dbc:h2:mem:///mysql;DB_CLOSE_DELAY=-1;MODE=MySQL;`
     */
    H2_MYSQL(
        connection = { "r2dbc:h2:mem:///mysql;DB_CLOSE_DELAY=-1;MODE=MySQL;" },
        driver = "org.h2.Driver",
        beforeConnection = {
            Mode::class.declaredMemberProperties
                .firstOrNull { it.name == "convertInsertNullToZero" }
                ?.let { field ->
                    val mode = Mode.getInstance("MySQL")
                    @Suppress("UNCHECKED_CAST")
                    (field as KMutableProperty1<Mode, Boolean>).set(mode, false)
                }
        }
    ),

    /**
     * MariaDB 호환 모드로 실행되는 H2 인메모리 데이터베이스.
     *
     * R2DBC 연결 문자열: `r2dbc:h2:mem:///mariadb;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;`
     */
    H2_MARIADB(
        connection = {
            "r2dbc:h2:mem:///mariadb;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"
        },
        driver = "org.h2.Driver",
    ),

    /**
     * PostgreSQL 호환 모드로 실행되는 H2 인메모리 데이터베이스.
     *
     * R2DBC 연결 문자열: `r2dbc:h2:mem:///psql;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;`
     */
    H2_PSQL(
        connection = {
            "r2dbc:h2:mem:///psql;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;"
        },
        driver = "org.h2.Driver"
    ),
//    H2_ORACLE(
//        connection = {
//            "r2dbc:h2:mem:///oracle;MODE=Oracle;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;"
//        },
//        driver = "org.h2.Driver"
//    ),
//    H2_SQLSERVER(
//        connection = { "r2dbc:h2:mem:///sqlserver;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;" },
//        driver = "org.h2.Driver"
//    ),

    /**
     * MariaDB 데이터베이스 (Testcontainers 또는 로컬 서버).
     *
     * [TestDBConfig.useTestcontainers]가 `true`이면 [Containers.MariaDB]를 사용하고,
     * `false`이면 로컬 3306 포트의 `exposed` 데이터베이스에 연결합니다.
     */
    MARIADB(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&useLegacyDatetimeCode=false&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&zeroDateTimeBehavior=convertToNull"  // +
            // "&rewriteBatchedStatements=true"

            if (useTestcontainers) {
                val port = Containers.MariaDB.port
                val databaseName = Containers.MariaDB.databaseName
                "r2dbc:mariadb://${MARIADB.user}:${MARIADB.pass}@127.0.0.1:$port/$databaseName$options"
            } else {
                "r2dbc:mariadb://localhost:3306/exposed$options"
            }
        },
        driver = "org.mariadb.jdbc.Driver",
    ),

    /**
     * MySQL 5.x 데이터베이스 (Testcontainers 또는 로컬 서버).
     *
     * **주의**: R2DBC 드라이버 호환성 문제로 인해 [enabledDialects] 목록에서 제외됩니다.
     * 활성화가 필요하면 테스트 클래스의 `databases()` 메서드에서 직접 지정하세요.
     */
    MYSQL_V5(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&zeroDateTimeBehavior=convertToNull"  // +
            // "&rewriteBatchedStatements=true"
            if (useTestcontainers) {
                val port = Containers.MySQL5.port
                val databaseName = Containers.MySQL5.databaseName
                "r2dbc:mysql://${MYSQL_V5.user}:${MYSQL_V5.pass}@127.0.0.1:$port/$databaseName$options"
            } else {
                "r2dbc:mysql://localhost:3306/exposed$options"
            }
        },
        driver = "com.mysql.cj.jdbc.Driver",
    ),

    /**
     * MySQL 8.x 데이터베이스 (Testcontainers 또는 로컬 서버).
     *
     * [TestDBConfig.useTestcontainers]가 `true`이면 [Containers.MySQL8]을 사용하고,
     * `false`이면 로컬 3306 포트의 `exposed` 데이터베이스에 연결합니다.
     */
    MYSQL_V8(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&zeroDateTimeBehavior=convertToNull" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&allowPublicKeyRetrieval=true" //  "&rewriteBatchedStatements=true" // Batch 처리를 위한 설정

            if (useTestcontainers) {
                val port = Containers.MySQL8.port
                val databaseName = Containers.MySQL8.databaseName
                "r2dbc:mysql://${MYSQL_V8.user}:${MYSQL_V8.pass}@127.0.0.1:$port/$databaseName$options"
            } else {
                "r2dbc:mysql://localhost:3306/exposed$options"
            }
        },
        driver = "com.mysql.cj.jdbc.Driver",
        user = if (useTestcontainers) "test" else "exposed",
        pass = if (useTestcontainers) "test" else "@exposed2025",
    ),

    /**
     * PostgreSQL 데이터베이스 (Testcontainers 또는 로컬 서버).
     *
     * [TestDBConfig.useTestcontainers]가 `true`이면 [Containers.Postgres]를 사용하고,
     * `false`이면 로컬 5432 포트의 `exposed` 데이터베이스에 연결합니다.
     */
    POSTGRESQL(
        connection = {
            val options = "?lc_messages=en_US.UTF-8"
            if (useTestcontainers) {
                val port = Containers.Postgres.port
                "r2dbc:postgresql://${POSTGRESQL.user}:${POSTGRESQL.pass}@127.0.0.1:$port/postgres$options"
            } else {
                "r2dbc:postgresql://localhost:5432/exposed$options"
            }
        },
        driver = "org.postgresql.Driver",
        user = if (useTestcontainers) "test" else "exposed",
//        afterConnection = { connection ->
//            connection.createStatement().use { stmt ->
//                stmt.execute("SET TIMEZONE='UTC';")
//            }
//        }
    );

    /** 이 항목에 대한 [R2dbcDatabase] 인스턴스입니다. 첫 [connect] 호출 시 초기화됩니다. */
    var db: R2dbcDatabase? = null

    /**
     * 이 [TestDB] 항목에 대한 [R2dbcDatabase] 연결을 생성하고 [db]에 저장한 뒤 반환합니다.
     *
     * @param configure 연결 전에 적용할 추가 [R2dbcDatabaseConfig.Builder] 설정 람다
     * @return 생성된 [R2dbcDatabase] 인스턴스
     */
    fun connect(configure: R2dbcDatabaseConfig.Builder.() -> Unit = {}): R2dbcDatabase {
        val config = R2dbcDatabaseConfig {
            dbConfig()
            configure()

            setUrl(connection())
        }
        val database = R2dbcDatabase.connect(databaseConfig = config)
        db = database
        return database
    }

    companion object: KLogging() {
        /** H2 계열 인메모리 DB 목록 (H2, H2_MYSQL, H2_PSQL, H2_MARIADB). */
        val ALL_H2 = setOf(H2, H2_MYSQL, H2_PSQL, H2_MARIADB /*H2_ORACLE, H2_SQLSERVER*/)

        /** MariaDB 전용 목록 (MARIADB). */
        val ALL_MARIADB = setOf(MARIADB)

        /** MariaDB 호환 DB 목록 (MARIADB, H2_MARIADB). */
        val ALL_MARIADB_LIKE = setOf(MARIADB, H2_MARIADB)

        /** MySQL 계열 DB 목록 (MYSQL_V5, MYSQL_V8). */
        val ALL_MYSQL = setOf(MYSQL_V5, MYSQL_V8)

        /** MySQL + MariaDB 목록. */
        val ALL_MYSQL_MARIADB = ALL_MYSQL + ALL_MARIADB

        /** MySQL 호환 DB 목록 (MYSQL_V5, MYSQL_V8, H2_MYSQL). */
        val ALL_MYSQL_LIKE = ALL_MYSQL + H2_MYSQL

        /** MySQL + MariaDB 호환 DB 전체 목록. */
        val ALL_MYSQL_MARIADB_LIKE = ALL_MYSQL_LIKE + ALL_MARIADB_LIKE

        /** PostgreSQL 전용 목록 (POSTGRESQL). */
        val ALL_POSTGRES = setOf(POSTGRESQL)

        /** PostgreSQL 호환 DB 목록 (POSTGRESQL, H2_PSQL). */
        val ALL_POSTGRES_LIKE = setOf(POSTGRESQL, H2_PSQL)
//        val ALL_ORACLE_LIKE = setOf(H2_ORACLE)
//        val ALL_SQLSERVER_LIKE = setOf(H2_SQLSERVER)

        /** 정의된 모든 [TestDB] 항목의 집합. */
        val ALL = TestDB.entries.toSet()

        /**
         * 현재 설정([TestDBConfig])에 따라 활성화된 dialect 집합을 반환합니다.
         *
         * - [TestDBConfig.useFastDB]가 `true`이면 H2만 반환합니다 (빠른 로컬 테스트).
         * - `false`이면 H2, PostgreSQL, MySQL V8을 반환합니다 (Testcontainers 필요).
         *
         * **참고**: [MYSQL_V5]는 R2DBC 드라이버 호환성 문제로 항상 제외됩니다.
         */
        fun enabledDialects(): Set<TestDB> {
            return if (useFastDB) setOf(H2)
            else setOf(TestDB.H2, TestDB.POSTGRESQL, TestDB.MYSQL_V8)
        }
    }
}
