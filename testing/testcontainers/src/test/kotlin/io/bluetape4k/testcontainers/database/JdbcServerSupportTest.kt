package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class JdbcServerSupportTest {
    companion object : KLogging()

    @Test
    fun `buildKebabJdbcProperties 는 kebab-case 키로 jdbc 관련 정보를 맵으로 제공한다`() {
        val server = jdbcServer()

        val props = server.buildKebabJdbcProperties()

        props["driver-class-name"] shouldBeEqualTo "org.postgresql.Driver"
        props["jdbc-url"] shouldBeEqualTo "jdbc:postgresql://localhost:5432/testdb"
        props["username"] shouldBeEqualTo "testuser"
        props["password"] shouldBeEqualTo "testpass"
        props["database-name"] shouldBeEqualTo "testdb"
    }

    @Suppress("DEPRECATION")
    @Test
    fun `buildJdbcProperties deprecated 함수는 기존 형식의 jdbc 관련 정보를 맵으로 제공한다`() {
        val server = jdbcServer()

        val props = server.buildJdbcProperties()

        props["driver-class-name"] shouldBeEqualTo "org.postgresql.Driver"
        props["jdbc-url"] shouldBeEqualTo "jdbc:postgresql://localhost:5432/testdb"
        props["username"] shouldBeEqualTo "testuser"
        props["password"] shouldBeEqualTo "testpass"
        props["database"] shouldBeEqualTo "testdb"
    }

    @Test
    fun `getDataSource 는 기본 설정과 builder 설정을 반영한다`() {
        val server = jdbcServer()

        server
            .getDataSource {
                maximumPoolSize = 3
                connectionTimeout = 5_000
                initializationFailTimeout = -1
            }.use { ds ->
                ds.driverClassName shouldBeEqualTo "org.postgresql.Driver"
                ds.jdbcUrl shouldBeEqualTo "jdbc:postgresql://localhost:5432/testdb"
                ds.username shouldBeEqualTo "testuser"
                ds.password shouldBeEqualTo "testpass"
                ds.maximumPoolSize shouldBeEqualTo 3
                ds.connectionTimeout shouldBeEqualTo 5_000
            }
    }

    @Test
    fun `getHikariDataSource 는 getDataSource 와 동일한 값을 제공한다`() {
        val server = jdbcServer()

        server
            .getHikariDataSource {
                minimumIdle = 1
                initializationFailTimeout = -1
            }.use { ds ->
                ds.driverClassName shouldBeEqualTo "org.postgresql.Driver"
                ds.jdbcUrl shouldBeEqualTo "jdbc:postgresql://localhost:5432/testdb"
                ds.username shouldBeEqualTo "testuser"
                ds.password shouldBeEqualTo "testpass"
                ds.minimumIdle shouldBeEqualTo 1
            }
    }

    private fun jdbcServer(): JdbcServer =
        mockk(relaxed = true) {
            every { getDriverClassName() } returns "org.postgresql.Driver"
            every { getJdbcUrl() } returns "jdbc:postgresql://localhost:5432/testdb"
            every { getUsername() } returns "testuser"
            every { getPassword() } returns "testpass"
            every { getDatabaseName() } returns "testdb"
        }
}
