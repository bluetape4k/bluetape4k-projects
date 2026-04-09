package io.bluetape4k.jdbc.hikari

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

/**
 * DSL 람다로 [HikariConfig]를 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val config = hikariConfigOf {
 *     jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
 *     username = "user"
 *     password = "secret"
 *     maximumPoolSize = 50
 *     minimumIdle = 5
 *     connectionTimeout = 30_000
 *     idleTimeout = 600_000
 *     maxLifetime = 1_800_000
 * }
 * ```
 *
 * @param init [HikariConfig] 설정 람다
 * @return 생성된 [HikariConfig]
 */
inline fun hikariConfigOf(
    init: HikariConfig.() -> Unit,
): HikariConfig = HikariConfig().apply(init)

/**
 * DSL 람다로 [HikariDataSource]를 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * val ds = hikariDataSourceOf {
 *     jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
 *     username = "user"
 *     password = "secret"
 *     maximumPoolSize = 50
 *     minimumIdle = 5
 *     poolName = "myPool"
 * }
 * ```
 *
 * @param init [HikariConfig] 설정 람다
 * @return 생성된 [HikariDataSource]
 */
inline fun hikariDataSourceOf(
    init: HikariConfig.() -> Unit,
): HikariDataSource = HikariDataSource(hikariConfigOf(init))

/**
 * JDBC URL과 선택적 DSL 람다로 [HikariDataSource]를 생성합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * // 기본 설정
 * val ds = hikariDataSourceOf("jdbc:postgresql://localhost:5432/mydb", "user", "secret")
 *
 * // 추가 설정 포함
 * val ds = hikariDataSourceOf(
 *     jdbcUrl = "jdbc:mysql://localhost:3306/mydb",
 *     username = "root",
 *     password = "secret",
 * ) {
 *     maximumPoolSize = 50
 *     minimumIdle = 5
 *     connectionTimeout = 30_000
 *     poolName = "myPool"
 * }
 * ```
 *
 * @param jdbcUrl JDBC URL 문자열
 * @param username 데이터베이스 접속 사용자 이름
 * @param password 데이터베이스 접속 비밀번호
 * @param init [HikariConfig] 추가 설정 람다 (생략 시 기본값 사용)
 * @return 생성된 [HikariDataSource]
 */
inline fun hikariDataSourceOf(
    jdbcUrl: String,
    username: String = "",
    password: String = "",
    init: HikariConfig.() -> Unit = {},
): HikariDataSource = hikariDataSourceOf {
    this.jdbcUrl = jdbcUrl
    if (username.isNotEmpty()) this.username = username
    if (password.isNotEmpty()) this.password = password
    init()
}
