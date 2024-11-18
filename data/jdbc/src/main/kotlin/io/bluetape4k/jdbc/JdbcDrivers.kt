package io.bluetape4k.jdbc

/**
 * Jdbc Driver Constants
 */
object JdbcDrivers {

    /**
     * H2 DataSource class name
     */
    const val DATASOURCE_CLASS_H2 = "org.h2.jdbcx.JdbcDataSource"

    /**
     * H2 Jdbc driver class name
     */
    const val DRIVER_CLASS_H2 = "org.h2.Driver"

    /**
     * H2 hibernate dialect
     */
    const val DIALECT_H2 = "org.hibernate.dialect.H2Dialect"

    /**
     * hsqldb DB DataSource class name
     */
    const val DATASOURCE_CLASS_HSQL = "org.hsqldb.jdbc.JDBCDataSource"

    /**
     * hsqldb Jdbc driver class name
     */
    const val DRIVER_CLASS_HSQL = "org.hsqldb.jdbc.JDBCDriver"

    /**
     * hsqldb hibernate dialect
     */
    const val DIALECT_HSQL = "org.hibernate.dialect.HSQLDialect"

    /**
     * MySQL DB DataSource class name
     */
    const val DATASOURCE_CLASS_MYSQL = "com.mysql.cj.jdbc.MysqlDataSource"

    /**
     * MySQL Jdbc driver class name
     */
    const val DRIVER_CLASS_MYSQL = "com.mysql.cj.jdbc.Driver"

    /**
     * MySQL hibernate dialect
     */
    const val DIALECT_MYSQL = "org.hibernate.dialect.MySQL5InnoDBDialect"

    /**
     * Maria DB DataSource class name
     */
    const val DRIVER_CLASS_MARIADB = "org.mariadb.jdbc.Driver"

    /**
     * PostgreSql DB DataSource class name
     */
    const val DATASOURCE_CLASS_POSTGRESQL = "org.postgresql.ds.PGSimpleDataSource"

    /**
     * PostgreSql Jdbc driver class name
     */
    const val DRIVER_CLASS_POSTGRESQL = "org.postgresql.Driver"

    /**
     * PostgreSql hibernate dialect for Postgresql 9.4 or higher
     */
    const val DIALECT_POSTGRESQL = "org.hibernate.dialect.PostgreSQL94Dialect"

    /**
     * PostgreSql hibernate dialect for Postgresql 9.0 or higher
     */
    const val DIALECT_POSTGRESQL9 = "org.hibernate.dialect.PostgreSQL9Dialect"

    /**
     * PostgreSql hibernate dialect for Postgresql 8.2 or higher
     */
    const val DIALECT_POSTGRESQL82 = "org.hibernate.dialect.PostgreSQL82Dialect"

    /**
     * Oracle DB DataSource class name
     */
    const val DATASOURCE_CLASS_ORACLE = "oracle.jdbc.pool.OracleDataSource"

    /**
     * Oracle Jdbc driver class name
     */
    const val DRIVER_CLASS_ORACLE = "oracle.jdbc.driver.OracleDriver"

    /**
     * hibernate dialect for Oracle 12c or higher
     */
    const val DIALECT_ORACLE12 = "org.hibernate.dialect.Oracle12cDialect"

    /**
     * hibernate dialect for Oracle 9i or higher
     */
    const val DIALECT_ORACLE9i = "org.hibernate.dialect.Oracle9iDialect"

    /**
     * hibernate dialect for Oracle 10g or higher
     */
    const val DIALECT_ORACLE10g = "org.hibernate.dialect.Oracle10gDialect"

    /** driverClass 가 MySQL 인가? */
    @JvmStatic
    fun isMySQL(driverClassName: String? = null): Boolean {
        return driverClassName != null &&
                (driverClassName == DRIVER_CLASS_MYSQL ||
                        driverClassName == DRIVER_CLASS_MARIADB)
    }

    /** driverClass 가 PostgreSQL 인가? */
    @JvmStatic
    fun isPostgreSQL(driverClassName: String? = null): Boolean {
        return driverClassName != null && driverClassName == DRIVER_CLASS_POSTGRESQL
    }
}
