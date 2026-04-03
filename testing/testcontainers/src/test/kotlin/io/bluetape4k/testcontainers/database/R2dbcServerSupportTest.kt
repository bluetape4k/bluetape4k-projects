package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.r2dbc.spi.ConnectionFactoryOptions
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class R2dbcServerSupportTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Test
    fun `PostgreSQLServer getConnectionFactoryOptions 는 올바른 R2DBC 옵션을 반환한다`() {
        PostgreSQLServer().use { server ->
            server.start()
            val options = server.getConnectionFactoryOptions()
            assertR2dbcOptions(options, "postgresql", server.host, server.port, server.getUsername(), server.getPassword(), server.getDatabaseName())
        }
    }

    @Test
    fun `PostgisServer getConnectionFactoryOptions 는 올바른 R2DBC 옵션을 반환한다`() {
        PostgisServer().use { server ->
            server.start()
            val options = server.getConnectionFactoryOptions()
            assertR2dbcOptions(options, "postgresql", server.host, server.port, server.getUsername(), server.getPassword(), server.getDatabaseName())
        }
    }

    @Test
    fun `PgvectorServer getConnectionFactoryOptions 는 올바른 R2DBC 옵션을 반환한다`() {
        PgvectorServer().use { server ->
            server.start()
            val options = server.getConnectionFactoryOptions()
            assertR2dbcOptions(options, "postgresql", server.host, server.port, server.getUsername(), server.getPassword(), server.getDatabaseName())
        }
    }

    @Test
    fun `MySQL8Server getConnectionFactoryOptions 는 올바른 R2DBC 옵션을 반환한다`() {
        MySQL8Server().use { server ->
            server.start()
            val options = server.getConnectionFactoryOptions()
            assertR2dbcOptions(options, "mysql", server.host, server.port, server.getUsername(), server.getPassword(), server.getDatabaseName())
        }
    }

    @Test
    fun `MySQL5Server getConnectionFactoryOptions 는 올바른 R2DBC 옵션을 반환한다`() {
        MySQL5Server().use { server ->
            server.start()
            val options = server.getConnectionFactoryOptions()
            assertR2dbcOptions(options, "mysql", server.host, server.port, server.getUsername(), server.getPassword(), server.getDatabaseName())
        }
    }

    @Test
    fun `MariaDBServer getConnectionFactoryOptions 는 올바른 R2DBC 옵션을 반환한다`() {
        MariaDBServer().use { server ->
            server.start()
            val options = server.getConnectionFactoryOptions()
            assertR2dbcOptions(options, "mariadb", server.host, server.port, server.getUsername(), server.getPassword(), server.getDatabaseName())
        }
    }

    private fun assertR2dbcOptions(
        options: ConnectionFactoryOptions,
        expectedDriver: String,
        expectedHost: String,
        expectedPort: Int,
        expectedUser: String?,
        expectedPassword: String?,
        expectedDatabase: String?,
    ) {
        options.shouldNotBeNull()
        options.getValue(ConnectionFactoryOptions.DRIVER) shouldBeEqualTo expectedDriver
        options.getValue(ConnectionFactoryOptions.HOST) shouldBeEqualTo expectedHost
        options.getValue(ConnectionFactoryOptions.PORT) shouldBeEqualTo expectedPort
        options.getValue(ConnectionFactoryOptions.USER) shouldBeEqualTo expectedUser
        options.getValue(ConnectionFactoryOptions.PASSWORD) shouldBeEqualTo expectedPassword
        options.getValue(ConnectionFactoryOptions.DATABASE) shouldBeEqualTo expectedDatabase
    }
}
