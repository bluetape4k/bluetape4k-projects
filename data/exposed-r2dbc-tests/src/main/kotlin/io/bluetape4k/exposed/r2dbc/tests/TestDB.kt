package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.logging.KLogging
import io.r2dbc.spi.IsolationLevel
import org.h2.engine.Mode
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties

/**
 * Postgres, MySQL 등의 서버를 Testconstainers 를 이용할 것인가? 직접 설치한 서버를 사용할 것인가?
 */
const val USE_TESTCONTAINERS = true

/**
 * true 이면, InMemory DB만을 대상으로 테스트 합니다.
 * false 이면 Postgres, MySQL V8 도 포함해서 테스트 합니다.
 */
const val USE_FAST_DB = false

/**
 * Exposed 기능을 테스트하기 위한 대상 DB 들의 목록과 정보들을 제공합니다.
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
     * H2 v2.+ 를 사용할 때
     */
    H2(
        connection = { "r2dbc:pool:h2:mem:///regular;DB_CLOSE_DELAY=-1;" },
        driver = "org.h2.Driver",
        dbConfig = {
            defaultR2dbcIsolationLevel = IsolationLevel.READ_COMMITTED
        }
    ),
    H2_MYSQL(
        connection = { "r2dbc:pool:h2:mem:///mysql;DB_CLOSE_DELAY=-1;MODE=MySQL;" },
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
    H2_MARIADB(
        connection = {
            "r2dbc:pool:h2:mem:///mariadb;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"
        },
        driver = "org.h2.Driver",
    ),
    H2_PSQL(
        connection = {
            "r2dbc:pool:h2:mem:///psql;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;"
        },
        driver = "org.h2.Driver"
    ),
    H2_ORACLE(
        connection = {
            "r2dbc:pool:h2:mem:///oracle;MODE=Oracle;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;"
        },
        driver = "org.h2.Driver"
    ),
    H2_SQLSERVER(
        connection = { "r2dbc:pool:h2:mem:///sqlserver;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;" },
        driver = "org.h2.Driver"
    ),

    MARIADB(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&useLegacyDatetimeCode=false&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&zeroDateTimeBehavior=convertToNull"  // +
            // "&rewriteBatchedStatements=true"

            if (USE_TESTCONTAINERS) {
                val port = ContainerProvider.mariadb.port
                val databaseName = ContainerProvider.mariadb.databaseName
                "r2dbc:pool:mariadb://${MARIADB.user}:${MARIADB.pass}@127.0.0.1:$port/$databaseName$options"
            } else {
                "r2dbc:pool:mariadb://localhost:3306/exposed$options"
            }
        },
        driver = "org.mariadb.jdbc.Driver",
    ),

    MYSQL_V5(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&zeroDateTimeBehavior=convertToNull"  // +
            // "&rewriteBatchedStatements=true"
            if (USE_TESTCONTAINERS) {
                val port = ContainerProvider.mysql5.port
                val databaseName = ContainerProvider.mariadb.databaseName
                "r2dbc:pool:mysql://${MYSQL_V5.user}:${MYSQL_V5.pass}@127.0.0.1:$port/$databaseName$options"
            } else {
                "r2dbc:pool:mysql://localhost:3306/exposed$options"
            }
        },
        driver = "com.mysql.cj.jdbc.Driver",
    ),

    MYSQL_V8(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&zeroDateTimeBehavior=convertToNull" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&allowPublicKeyRetrieval=true" //  "&rewriteBatchedStatements=true"                        // Batch 처리를 위한 설정

            if (USE_TESTCONTAINERS) {
                val port = ContainerProvider.mysql8.port
                val databaseName = ContainerProvider.mariadb.databaseName
                "r2dbc:pool:mysql://${MYSQL_V8.user}:${MYSQL_V8.pass}@127.0.0.1:$port/$databaseName$options"
            } else {
                "r2dbc:pool:mysql://localhost:3306/exposed$options"
            }
        },
        driver = "com.mysql.cj.jdbc.Driver",
        user = if (USE_TESTCONTAINERS) "test" else "exposed",
        pass = if (USE_TESTCONTAINERS) "test" else "@exposed2025",
    ),

    POSTGRESQL(
        connection = {
            val options = "?lc_messages=en_US.UTF-8"
            if (USE_TESTCONTAINERS) {
                val port = ContainerProvider.postgres.port
                "r2dbc:pool:postgresql://${POSTGRESQL.user}:${POSTGRESQL.pass}@127.0.0.1:$port/postgres$options"
            } else {
                "r2dbc:pool:postgresql://localhost:5432/exposed$options"
            }
        },
        driver = "org.postgresql.Driver",
        user = if (USE_TESTCONTAINERS) "test" else "exposed",
//        afterConnection = { connection ->
//            connection.createStatement().use { stmt ->
//                stmt.execute("SET TIMEZONE='UTC';")
//            }
//        }
    );

    var db: R2dbcDatabase? = null

    fun connect(configure: R2dbcDatabaseConfig.Builder.() -> Unit = {}): R2dbcDatabase {
        val config = R2dbcDatabaseConfig {
            dbConfig()
            configure()

            setUrl(connection())
        }
        return R2dbcDatabase.connect(databaseConfig = config)
    }

    companion object: KLogging() {
        val ALL_H2 = setOf(H2, H2_MYSQL, H2_PSQL, H2_MARIADB /*H2_ORACLE, H2_SQLSERVER*/)
        val ALL_MARIADB = setOf(MARIADB)
        val ALL_MARIADB_LIKE = setOf(MARIADB, H2_MARIADB)
        val ALL_MYSQL = setOf(MYSQL_V5, MYSQL_V8)
        val ALL_MYSQL_MARIADB = ALL_MYSQL + ALL_MARIADB
        val ALL_MYSQL_LIKE = ALL_MYSQL + H2_MYSQL
        val ALL_MYSQL_MARIADB_LIKE = ALL_MYSQL_LIKE + ALL_MARIADB_LIKE
        val ALL_POSTGRES = setOf(POSTGRESQL)
        val ALL_POSTGRES_LIKE = setOf(POSTGRESQL, H2_PSQL)
        val ALL_ORACLE_LIKE = setOf(H2_ORACLE)
        val ALL_SQLSERVER_LIKE = setOf(H2_SQLSERVER)

        val ALL = TestDB.entries.toSet()

        // NOTE: 이 값을 바꿔서 MySQL, PostgreSQL 등을 testcontainers 를 이용하여 테스트할 수 있습니다.

        fun enabledDialects(): Set<TestDB> {
            return if (USE_FAST_DB) ALL_H2
            else ALL_H2 + ALL_POSTGRES + ALL_MYSQL_MARIADB - MYSQL_V5 // MySQL 5.7 과 MySQL 8.0 이 Driver의 버전이 다름 
        }
    }
}
