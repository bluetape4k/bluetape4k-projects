package io.bluetape4k.exposed.tests

import io.bluetape4k.exposed.tests.TestDBConfig.useFastDB
import io.bluetape4k.exposed.tests.TestDBConfig.useTestcontainers
import io.bluetape4k.jdbc.JdbcDrivers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection
import kotlin.reflect.full.declaredMemberProperties


/**
 * Exposed 기능을 테스트하기 위한 대상 DB 들의 목록과 정보들을 제공합니다.
 */

enum class TestDB(
    val connection: () -> String,
    val driver: String,
    val user: String = "test",
    val pass: String = "test",
    val beforeConnection: () -> Unit = {},
    val afterConnection: (connection: Connection) -> Unit = {},
    val afterTestFinished: () -> Unit = {},
    val dbConfig: DatabaseConfig.Builder.() -> Unit = {},
) {
    /**
     * H2 v1.+ 를 사용할 때
     */
    H2_V1(
        connection = { "jdbc:h2:mem:regular-v1;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;" },
        driver = JdbcDrivers.DRIVER_CLASS_H2,
        dbConfig = {
            defaultIsolationLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED
        }
    ),

    /**
     * H2 v2.+ 를 사용할 때
     */
    H2(
        connection = { "jdbc:h2:mem:regular-v2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;" },
        driver = JdbcDrivers.DRIVER_CLASS_H2,
        dbConfig = {
            defaultIsolationLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED
        }
    ),
    /**
     * H2 데이터베이스를 MySQL 호환 모드로 실행합니다.
     *
     * MySQL 특유의 `convertInsertNullToZero` 동작을 비활성화하여 NULL 삽입이 올바르게 처리됩니다.
     */
    H2_MYSQL(
        connection = {
            "jdbc:h2:mem:mysql;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;"
        },
        driver = JdbcDrivers.DRIVER_CLASS_H2,
        beforeConnection = {
            org.h2.engine.Mode::class.declaredMemberProperties
                .firstOrNull { it.name == "convertInsertNullToZero" }
                ?.let { field ->
                    val mode = org.h2.engine.Mode.getInstance("MySQL")
                    @Suppress("UNCHECKED_CAST")
                    (field as kotlin.reflect.KMutableProperty1<org.h2.engine.Mode, Boolean>).set(mode, false)
                }
        }
    ),
    /**
     * H2 데이터베이스를 MariaDB 호환 모드로 실행합니다.
     *
     * `DATABASE_TO_LOWER=TRUE` 옵션으로 데이터베이스 이름이 소문자로 변환됩니다.
     */
    H2_MARIADB(
        connection = {
            "jdbc:h2:mem:mariadb;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;"
        },
        driver = JdbcDrivers.DRIVER_CLASS_H2,
    ),

    /**
     * H2 데이터베이스를 PostgreSQL 호환 모드로 실행합니다.
     *
     * `DATABASE_TO_LOWER=TRUE` 및 `DEFAULT_NULL_ORDERING=HIGH` 옵션이 적용됩니다.
     */
    H2_PSQL(
        connection = {
            "jdbc:h2:mem:psql;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;"
        },
        driver = JdbcDrivers.DRIVER_CLASS_H2
    ),

    /**
     * Testcontainers 또는 로컬 MariaDB 서버에 연결합니다.
     *
     * UTF-8 인코딩, UTC 타임존, Batch 처리를 위한 옵션이 자동으로 설정됩니다.
     */
    MARIADB(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&useLegacyDatetimeCode=false&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&zeroDateTimeBehavior=convertToNull" +
                    "&rewriteBatchedStatements=true" // Batch 처리를 위한 설정

            if (useTestcontainers) {
                Containers.MariaDB.jdbcUrl + options
            } else {
                "jdbc:mariadb://localhost:3306/exposed$options"
            }
        },
        driver = JdbcDrivers.DRIVER_CLASS_MARIADB,
    ),

    /**
     * Testcontainers 또는 로컬 MySQL 5.7 서버에 연결합니다.
     *
     * UTF-8 인코딩, UTC 타임존, Batch 처리를 위한 옵션이 자동으로 설정됩니다.
     */
    MYSQL_V5(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&zeroDateTimeBehavior=convertToNull" +
                    "&rewriteBatchedStatements=true" // Batch 처리를 위한 설정

            if (useTestcontainers) {
                Containers.MySQL5.jdbcUrl + options
            } else {
                "jdbc:mysql://localhost:3306/exposed$options"
            }
        },
        driver = JdbcDrivers.DRIVER_CLASS_MYSQL
    ),

    /**
     * Testcontainers 또는 로컬 MySQL 8.0 서버에 연결합니다.
     *
     * `allowPublicKeyRetrieval=true` 옵션이 추가되어 MySQL 8.0 인증 방식을 지원합니다.
     */
    MYSQL_V8(
        connection = {
            val options = "?useSSL=false" +
                    "&characterEncoding=UTF-8" +
                    "&zeroDateTimeBehavior=convertToNull" +
                    "&useLegacyDatetimeCode=false" +
                    "&serverTimezone=UTC" +  // TimeZone 을 UTC 로 설정
                    "&allowPublicKeyRetrieval=true" +
                    "&rewriteBatchedStatements=true" // Batch 처리를 위한 설정

            if (useTestcontainers) {
                Containers.MySQL8.jdbcUrl + options
            } else {
                "jdbc:mysql://localhost:3306/exposed$options"
            }
        },
        driver = JdbcDrivers.DRIVER_CLASS_MYSQL,
        user = if (useTestcontainers) "test" else "exposed",
        pass = if (useTestcontainers) "test" else "@exposed2025"
    ),

    /**
     * Testcontainers 또는 로컬 PostgreSQL 서버에 연결합니다.
     *
     * 연결 후 `SET TIMEZONE='UTC'`를 실행하여 타임존을 UTC로 강제 설정합니다.
     */
    POSTGRESQL(
        connection = {
            val options = "?lc_messages=en_US.UTF-8"
            if (useTestcontainers) {
                Containers.Postgres.jdbcUrl + options
            } else {
                "jdbc:postgresql://localhost:5432/exposed$options"
            }
        },
        driver = JdbcDrivers.DRIVER_CLASS_POSTGRESQL,
        user = if (useTestcontainers) "test" else "exposed",
        afterConnection = { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute("SET TIMEZONE='UTC';")
            }
        }
    ),

    /**
     * pgjdbc-ng 드라이버를 사용하여 PostgreSQL 서버에 연결합니다.
     *
     * [POSTGRESQL]과 동일한 연결 URL을 사용하되 `:postgresql:` 부분을 `:pgsql:`로 대체합니다.
     * 연결 후 `SET TIMEZONE='UTC'`를 실행하여 타임존을 UTC로 강제 설정합니다.
     */
    POSTGRESQLNG(
        connection = { POSTGRESQL.connection().replace(":postgresql:", ":pgsql:") },
        driver = "com.impossibl.postgres.jdbc.PGDriver",
        user = if (useTestcontainers) "test" else "exposed",
        afterConnection = { connection ->
            connection.createStatement().use { stmt ->
                stmt.execute("SET TIMEZONE='UTC';")
            }
        }
    );

    /** 현재 TestDB에 연결된 [Database] 인스턴스. 아직 연결되지 않은 경우 `null`입니다. */
    var db: Database? = null

    /**
     * 이 [TestDB]에 해당하는 데이터베이스에 연결하고 [Database] 인스턴스를 반환합니다.
     *
     * 연결 전에 [beforeConnection]을 실행하고, 연결 후 [afterConnection]을 실행합니다.
     * [configure] 람다를 통해 추가적인 [DatabaseConfig] 설정을 적용할 수 있습니다.
     *
     * @param configure 추가 [DatabaseConfig] 설정 람다 (선택적)
     * @return 생성된 [Database] 인스턴스
     */
    fun connect(configure: DatabaseConfig.Builder.() -> Unit = {}): Database {
        beforeConnection()
        val config = DatabaseConfig {
            dbConfig()
            configure()
        }
        val database = Database.connect(
            url = connection(),
            databaseConfig = config,
            user = user,
            password = pass,
            driver = driver,
            setupConnection = { afterConnection(it) },
        )
        db = database
        return database
    }

    /**
     * Batch 처리를 지원하는 [Database] 인스턴스를 반환합니다.
     *
     * MySQL/MariaDB 계열의 경우 `allowMultiQueries=true` 옵션을 추가하여 새 연결을 생성합니다.
     * 그 외 DB는 기존 [db] 인스턴스를 재사용하거나 [connect]로 연결합니다.
     *
     * @return Batch 쿼리를 지원하는 [Database] 인스턴스, 또는 `null`
     */
    fun getDatabaseForBatch(): Database? {
        if (this !in TestDB.ALL_MYSQL_MARIADB) {
            return this.db ?: connect()
        }

        val extra = if (this in TestDB.ALL_MARIADB) "?" else ""
        return Database.connect(
            this.connection().plus("$extra&allowMultiQueries=true"),
            this.driver,
            this.user,
            this.pass
        )
    }

    companion object: KLogging() {
        /** H2 v1 데이터베이스만 포함하는 집합입니다. */
        val ALL_H2_V1 = setOf(H2_V1)

        /** 모든 H2 변종(기본, MySQL 모드, PostgreSQL 모드, MariaDB 모드)을 포함하는 집합입니다. */
        val ALL_H2 = setOf(H2, H2_MYSQL, H2_PSQL, H2_MARIADB)

        /** 실제 MariaDB 서버만 포함하는 집합입니다. */
        val ALL_MARIADB = setOf(MARIADB)

        /** MariaDB 호환 DB를 포함하는 집합입니다: MariaDB, H2 MariaDB 모드. */
        val ALL_MARIADB_LIKE = setOf(MARIADB, H2_MARIADB)

        /** 실제 MySQL 서버(v5, v8)만 포함하는 집합입니다. */
        val ALL_MYSQL = setOf(MYSQL_V5, MYSQL_V8)

        /** MySQL과 MariaDB 실제 서버를 포함하는 집합입니다. */
        val ALL_MYSQL_MARIADB = ALL_MYSQL + ALL_MARIADB

        /** MySQL 호환 DB를 포함하는 집합입니다: MySQL v5/v8, MariaDB, H2 MySQL/MariaDB 모드. */
        val ALL_MYSQL_LIKE = ALL_MYSQL_MARIADB + H2_MYSQL + H2_MARIADB

        /** 실제 PostgreSQL 서버(표준 드라이버, pgjdbc-ng 드라이버)를 포함하는 집합입니다. */
        val ALL_POSTGRES = setOf(POSTGRESQL, POSTGRESQLNG)

        /** PostgreSQL 호환 DB를 포함하는 집합입니다: PostgreSQL, pgjdbc-ng, H2 PostgreSQL 모드. */
        val ALL_POSTGRES_LIKE = setOf(POSTGRESQL, POSTGRESQLNG, H2_PSQL)

        /** 모든 지원 DB를 포함하는 집합입니다. */
        val ALL = TestDB.entries.toSet()

        /**
         * 현재 테스트 환경에서 활성화된 DB 목록을 반환합니다.
         *
         * - [useFastDB]가 `true`이면 H2만 반환합니다 (빠른 로컬 테스트).
         * - [useFastDB]가 `false`이면 H2, PostgreSQL, MySQL 8.0을 반환합니다 (Docker 필요).
         *
         * @return 현재 환경에서 활성화된 [TestDB] 집합
         */
        fun enabledDialects(): Set<TestDB> {
            return if (useFastDB) setOf(H2)
            else setOf(TestDB.H2, TestDB.POSTGRESQL, TestDB.MYSQL_V8)
        }
    }
}
