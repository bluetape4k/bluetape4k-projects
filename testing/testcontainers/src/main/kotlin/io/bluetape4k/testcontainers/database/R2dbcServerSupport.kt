package io.bluetape4k.testcontainers.database

import io.r2dbc.spi.ConnectionFactoryOptions
import org.testcontainers.mariadb.MariaDBR2DBCDatabaseContainer
import org.testcontainers.mysql.MySQLR2DBCDatabaseContainer
import org.testcontainers.postgresql.PostgreSQLR2DBCDatabaseContainer
import org.testcontainers.mariadb.MariaDBContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * [PostgreSQLServer]로부터 R2DBC [ConnectionFactoryOptions]를 생성합니다.
 *
 * ```kotlin
 * val server = PostgreSQLServer().apply { start() }
 * val options = server.getConnectionFactoryOptions()
 * val connectionFactory = ConnectionFactories.get(options)
 * ```
 */
fun PostgreSQLServer.getConnectionFactoryOptions(): ConnectionFactoryOptions =
    PostgreSQLR2DBCDatabaseContainer.getOptions(this as PostgreSQLContainer)

/**
 * [PostgisServer]로부터 R2DBC [ConnectionFactoryOptions]를 생성합니다.
 *
 * ```kotlin
 * val server = PostgisServer().apply { start() }
 * val options = server.getConnectionFactoryOptions()
 * val connectionFactory = ConnectionFactories.get(options)
 * ```
 */
fun PostgisServer.getConnectionFactoryOptions(): ConnectionFactoryOptions =
    PostgreSQLR2DBCDatabaseContainer.getOptions(this as PostgreSQLContainer)

/**
 * [PgvectorServer]로부터 R2DBC [ConnectionFactoryOptions]를 생성합니다.
 *
 * ```kotlin
 * val server = PgvectorServer().apply { start() }
 * val options = server.getConnectionFactoryOptions()
 * val connectionFactory = ConnectionFactories.get(options)
 * ```
 */
fun PgvectorServer.getConnectionFactoryOptions(): ConnectionFactoryOptions =
    PostgreSQLR2DBCDatabaseContainer.getOptions(this as PostgreSQLContainer)

/**
 * [MySQL8Server]로부터 R2DBC [ConnectionFactoryOptions]를 생성합니다.
 *
 * ```kotlin
 * val server = MySQL8Server().apply { start() }
 * val options = server.getConnectionFactoryOptions()
 * val connectionFactory = ConnectionFactories.get(options)
 * ```
 */
fun MySQL8Server.getConnectionFactoryOptions(): ConnectionFactoryOptions =
    MySQLR2DBCDatabaseContainer.getOptions(this as MySQLContainer)

/**
 * [MySQL5Server]로부터 R2DBC [ConnectionFactoryOptions]를 생성합니다.
 *
 * ```kotlin
 * val server = MySQL5Server().apply { start() }
 * val options = server.getConnectionFactoryOptions()
 * val connectionFactory = ConnectionFactories.get(options)
 * ```
 */
fun MySQL5Server.getConnectionFactoryOptions(): ConnectionFactoryOptions =
    MySQLR2DBCDatabaseContainer.getOptions(this as MySQLContainer)

/**
 * [MariaDBServer]로부터 R2DBC [ConnectionFactoryOptions]를 생성합니다.
 *
 * ```kotlin
 * val server = MariaDBServer().apply { start() }
 * val options = server.getConnectionFactoryOptions()
 * val connectionFactory = ConnectionFactories.get(options)
 * ```
 */
fun MariaDBServer.getConnectionFactoryOptions(): ConnectionFactoryOptions =
    MariaDBR2DBCDatabaseContainer.getOptions(this as MariaDBContainer)
