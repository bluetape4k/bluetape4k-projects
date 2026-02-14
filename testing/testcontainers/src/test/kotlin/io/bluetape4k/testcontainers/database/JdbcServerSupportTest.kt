package io.bluetape4k.testcontainers.database

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JdbcServerSupportTest {

    @Test
    fun `buildJdbcProperties 는 jdbc 관련 정보를 맵으로 제공한다`() {
        val server = jdbcServer()

        val props = server.buildJdbcProperties()

        assertEquals("org.postgresql.Driver", props["driver-class-name"])
        assertEquals("jdbc:postgresql://localhost:5432/testdb", props["jdbc-url"])
        assertEquals("testuser", props["username"])
        assertEquals("testpass", props["password"])
        assertEquals("testdb", props["database"])
    }

    @Test
    fun `getDataSource 는 기본 설정과 builder 설정을 반영한다`() {
        val server = jdbcServer()

        server.getDataSource {
            maximumPoolSize = 3
            connectionTimeout = 5_000
            initializationFailTimeout = -1
        }.use { ds ->
            assertEquals("org.postgresql.Driver", ds.driverClassName)
            assertEquals("jdbc:postgresql://localhost:5432/testdb", ds.jdbcUrl)
            assertEquals("testuser", ds.username)
            assertEquals("testpass", ds.password)
            assertEquals(3, ds.maximumPoolSize)
            assertEquals(5_000, ds.connectionTimeout)
        }
    }

    @Test
    fun `getHikariDataSource 는 getDataSource 와 동일한 값을 제공한다`() {
        val server = jdbcServer()

        server.getHikariDataSource {
            minimumIdle = 1
            initializationFailTimeout = -1
        }.use { ds ->
            assertEquals("org.postgresql.Driver", ds.driverClassName)
            assertEquals("jdbc:postgresql://localhost:5432/testdb", ds.jdbcUrl)
            assertEquals("testuser", ds.username)
            assertEquals("testpass", ds.password)
            assertEquals(1, ds.minimumIdle)
        }
    }

    private fun jdbcServer(): JdbcServer = mockk(relaxed = true) {
        every { getDriverClassName() } returns "org.postgresql.Driver"
        every { getJdbcUrl() } returns "jdbc:postgresql://localhost:5432/testdb"
        every { getUsername() } returns "testuser"
        every { getPassword() } returns "testpass"
        every { getDatabaseName() } returns "testdb"
    }
}
