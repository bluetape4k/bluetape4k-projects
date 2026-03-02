package io.bluetape4k.jdbc

/**
 * 주요 JDBC 드라이버/데이터소스/Dialect 상수를 제공합니다.
 *
 * ## 동작/계약
 * - 상수는 문자열 리터럴이며 런타임 계산이나 추가 할당이 없습니다.
 * - `isMySQL`, `isPostgreSQL`는 전달 문자열과 정의된 드라이버 클래스명을 비교합니다.
 * - null 입력 시 두 판별 함수 모두 `false`를 반환합니다.
 *
 * ```kotlin
 * val mysql = JdbcDrivers.isMySQL("com.mysql.cj.jdbc.Driver")
 * val postgres = JdbcDrivers.isPostgreSQL("org.postgresql.Driver")
 * // mysql == true
 * // postgres == true
 * ```
 */
object JdbcDrivers {

    /** H2 DataSource 클래스명입니다. */
    const val DATASOURCE_CLASS_H2 = "org.h2.jdbcx.JdbcDataSource"

    /** H2 JDBC Driver 클래스명입니다. */
    const val DRIVER_CLASS_H2 = "org.h2.Driver"

    /** H2 Hibernate Dialect 클래스명입니다. */
    const val DIALECT_H2 = "org.hibernate.dialect.H2Dialect"

    /** HSQLDB DataSource 클래스명입니다. */
    const val DATASOURCE_CLASS_HSQL = "org.hsqldb.jdbc.JDBCDataSource"

    /** HSQLDB JDBC Driver 클래스명입니다. */
    const val DRIVER_CLASS_HSQL = "org.hsqldb.jdbc.JDBCDriver"

    /** HSQLDB Hibernate Dialect 클래스명입니다. */
    const val DIALECT_HSQL = "org.hibernate.dialect.HSQLDialect"

    /** MySQL DataSource 클래스명입니다. */
    const val DATASOURCE_CLASS_MYSQL = "com.mysql.cj.jdbc.MysqlDataSource"

    /** MySQL JDBC Driver 클래스명입니다. */
    const val DRIVER_CLASS_MYSQL = "com.mysql.cj.jdbc.Driver"

    /** MySQL Hibernate Dialect 클래스명입니다. */
    const val DIALECT_MYSQL = "org.hibernate.dialect.MySQL5InnoDBDialect"

    /** MariaDB JDBC Driver 클래스명입니다. */
    const val DRIVER_CLASS_MARIADB = "org.mariadb.jdbc.Driver"

    /** PostgreSQL DataSource 클래스명입니다. */
    const val DATASOURCE_CLASS_POSTGRESQL = "org.postgresql.ds.PGSimpleDataSource"

    /** PostgreSQL JDBC Driver 클래스명입니다. */
    const val DRIVER_CLASS_POSTGRESQL = "org.postgresql.Driver"

    /** PostgreSQL 9.4+ Hibernate Dialect 클래스명입니다. */
    const val DIALECT_POSTGRESQL = "org.hibernate.dialect.PostgreSQL94Dialect"

    /** PostgreSQL 9.0+ Hibernate Dialect 클래스명입니다. */
    const val DIALECT_POSTGRESQL9 = "org.hibernate.dialect.PostgreSQL9Dialect"

    /** PostgreSQL 8.2+ Hibernate Dialect 클래스명입니다. */
    const val DIALECT_POSTGRESQL82 = "org.hibernate.dialect.PostgreSQL82Dialect"

    /** Oracle DataSource 클래스명입니다. */
    const val DATASOURCE_CLASS_ORACLE = "oracle.jdbc.pool.OracleDataSource"

    /** Oracle JDBC Driver 클래스명입니다. */
    const val DRIVER_CLASS_ORACLE = "oracle.jdbc.driver.OracleDriver"

    /** Oracle 12c+ Hibernate Dialect 클래스명입니다. */
    const val DIALECT_ORACLE12 = "org.hibernate.dialect.Oracle12cDialect"

    /** Oracle 9i+ Hibernate Dialect 클래스명입니다. */
    const val DIALECT_ORACLE9i = "org.hibernate.dialect.Oracle9iDialect"

    /** Oracle 10g+ Hibernate Dialect 클래스명입니다. */
    const val DIALECT_ORACLE10g = "org.hibernate.dialect.Oracle10gDialect"

    /**
     * 전달한 드라이버 클래스명이 MySQL/MariaDB 계열인지 검사합니다.
     *
     * ## 동작/계약
     * - [driverClassName]이 `null`이면 `false`를 반환합니다.
     * - MySQL 또는 MariaDB 드라이버 클래스명과 정확히 일치할 때만 `true`를 반환합니다.
     * - 수신 객체를 변경하지 않고 상수 비교만 수행합니다.
     *
     * ```kotlin
     * val a = JdbcDrivers.isMySQL(JdbcDrivers.DRIVER_CLASS_MYSQL)
     * val b = JdbcDrivers.isMySQL(null)
     * // a == true
     * // b == false
     * ```
     */
    @JvmStatic
    fun isMySQL(driverClassName: String? = null): Boolean {
        return driverClassName != null &&
                (driverClassName == DRIVER_CLASS_MYSQL ||
                        driverClassName == DRIVER_CLASS_MARIADB)
    }

    /**
     * 전달한 드라이버 클래스명이 PostgreSQL 계열인지 검사합니다.
     *
     * ## 동작/계약
     * - [driverClassName]이 `null`이면 `false`를 반환합니다.
     * - PostgreSQL 드라이버 클래스명과 정확히 일치할 때만 `true`를 반환합니다.
     * - 비교 연산만 수행하며 추가 할당이 없습니다.
     *
     * ```kotlin
     * val a = JdbcDrivers.isPostgreSQL(JdbcDrivers.DRIVER_CLASS_POSTGRESQL)
     * val b = JdbcDrivers.isPostgreSQL(JdbcDrivers.DRIVER_CLASS_MYSQL)
     * // a == true
     * // b == false
     * ```
     */
    @JvmStatic
    fun isPostgreSQL(driverClassName: String? = null): Boolean {
        return driverClassName != null && driverClassName == DRIVER_CLASS_POSTGRESQL
    }
}
