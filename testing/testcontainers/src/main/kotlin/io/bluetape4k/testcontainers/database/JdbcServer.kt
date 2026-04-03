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
 * kebab-case 키를 사용하는 JDBC 프로퍼티 맵을 생성합니다.
 *
 * 시스템 프로퍼티 키 형식: `testcontainers.{namespace}.{kebab-case-key}`
 * 예: `testcontainers.postgresql.jdbc-url`, `testcontainers.postgresql.driver-class-name`
 *
 * **타입 변경 주의**: 기존 [buildJdbcProperties]는 `Map<String, Any?>`(null 값 포함)을 반환했으나,
 * 이 함수는 `Map<String, String>`(null 값 제외)을 반환합니다.
 * [writeToSystemProperties][io.bluetape4k.testcontainers.PropertyExportingServer.writeToSystemProperties]는
 * 기존에도 null 필터링했으므로 시스템 프로퍼티 등록에는 영향 없습니다.
 */
fun <T: JdbcServer> T.buildKebabJdbcProperties(): Map<String, String> = buildMap {
    put("jdbc-url", getJdbcUrl())
    put("driver-class-name", getDriverClassName())
    getUsername()?.let { put("username", it) }
    getPassword()?.let { put("password", it) }
    getDatabaseName()?.let { put("database-name", it) }
}

/**
 * [buildKebabJdbcProperties]의 deprecated alias입니다.
 *
 * @deprecated [buildKebabJdbcProperties]를 사용하세요.
 */
@Deprecated(
    message = "buildDotSeparatedJdbcProperties()는 deprecated. buildKebabJdbcProperties() 사용",
    replaceWith = ReplaceWith("buildKebabJdbcProperties()")
)
fun <T: JdbcServer> T.buildDotSeparatedJdbcProperties(): Map<String, String> = buildKebabJdbcProperties()

/**
 * [buildKebabJdbcProperties]의 deprecated alias입니다.
 *
 * @deprecated [buildKebabJdbcProperties]를 사용하세요.
 */
@Deprecated(
    message = "buildJdbcPropertiesCompat()는 deprecated. buildKebabJdbcProperties() 사용",
    replaceWith = ReplaceWith("buildKebabJdbcProperties()")
)
fun <T: JdbcServer> T.buildJdbcPropertiesCompat(): Map<String, String> = buildKebabJdbcProperties()

/**
 * [JdbcServer]의 Jdbc properties를 생성합니다.
 */
@Deprecated(
    message = "buildJdbcProperties()는 Map<String, Any?> 반환으로 인해 deprecated. buildKebabJdbcProperties() 사용",
    replaceWith = ReplaceWith("buildKebabJdbcProperties()")
)
fun <T: JdbcServer> T.buildJdbcProperties(): Map<String, Any?> {
    return mapOf(
        "driver-class-name" to getDriverClassName(),
        "jdbc-url" to getJdbcUrl(),
        "username" to getUsername(),
        "password" to getPassword(),
        "database" to getDatabaseName()
    )
}

private fun JdbcServer.newHikariConfig(builder: HikariConfig.() -> Unit): HikariConfig =
    HikariConfig().also {
        it.driverClassName = getDriverClassName()
        it.jdbcUrl = getJdbcUrl()
        it.username = getUsername()
        it.password = getPassword()
        it.apply(builder)
    }

/**
 * Database 접속을 위한 [HikariDataSource]를 제공합니다.
 */
fun <T: JdbcServer> T.getDataSource(
    builder: HikariConfig.() -> Unit = {},
): HikariDataSource {
    return HikariDataSource(newHikariConfig(builder))
}

/**
 * Database 접속을 위한 [HikariDataSource]를 제공합니다.
 * [getDataSource]의 별칭입니다.
 */
fun <T: JdbcServer> T.getHikariDataSource(
    builder: HikariConfig.() -> Unit = {},
): HikariDataSource {
    return getDataSource(builder)
}
