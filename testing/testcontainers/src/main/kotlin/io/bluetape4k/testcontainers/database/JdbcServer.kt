package io.bluetape4k.testcontainers.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.testcontainers.GenericServer

/**
 * Jdbc Server의 기본 정보를 제공합니다.
 */
interface JdbcServer: GenericServer {

    /**
     * Jdbc Driver class name을 제공합니다.
     */
    fun getDriverClassName(): String

    /**
     * Jdbc URL을 제공합니다.
     */
    fun getJdbcUrl(): String

    /**
     * Jdbc Username을 제공합니다.
     */
    fun getUsername(): String?

    /**
     * Jdbc Password를 제공합니다.
     */
    fun getPassword(): String?

    /**
     * Database 이름을 제공합니다.
     */
    fun getDatabaseName(): String?

}

/**
 * [JdbcServer]의 Jdbc properties를 생성합니다.
 */
fun <T: JdbcServer> T.buildJdbcProperties(): Map<String, Any?> {
    return mapOf(
        "driver-class-name" to getDriverClassName(),
        "jdbc-url" to getJdbcUrl(),
        "username" to getUsername(),
        "password" to getPassword(),
        "database" to getDatabaseName()
    )
}

/**
 * Database 접속을 위한 [HikariDataSource]를 제공합니다.
 */
fun <T: JdbcServer> T.getDataSource(
    initializer: HikariConfig.() -> Unit = {},
): HikariDataSource {
    val config = HikariConfig().also {
        it.driverClassName = getDriverClassName()
        it.jdbcUrl = getJdbcUrl()
        it.username = getUsername()
        it.password = getPassword()

        it.apply(initializer)
    }
    return HikariDataSource(config)
}

/**
 * Database 접속을 위한 [HikariDataSource]를 제공합니다.
 */
inline fun <T: JdbcServer> T.getHikariDataSource(
    initializer: HikariConfig.() -> Unit = {},
): HikariDataSource {
    val config = HikariConfig().also {
        it.driverClassName = getDriverClassName()
        it.jdbcUrl = getJdbcUrl()
        it.username = getUsername()
        it.password = getPassword()

        it.apply(initializer)
    }
    return HikariDataSource(config)
}
