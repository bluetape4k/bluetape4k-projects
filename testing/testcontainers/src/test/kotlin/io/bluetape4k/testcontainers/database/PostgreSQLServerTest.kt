package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PostgreSQLServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch PostgreSQL server`() {
            PostgreSQLServer().use { postgres ->
                postgres.start()

                assertConnection(postgres)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch PostgreSQL server with default port`() {
            PostgreSQLServer(useDefaultPort = true).use { postgres ->
                postgres.start()

                postgres.port shouldBeEqualTo PostgreSQLServer.PORT
                assertConnection(postgres)
            }
        }
    }

    @Nested
    inner class WithExtensions {
        @Test
        fun `withExtensions 으로 contrib 확장을 활성화한다`() {
            PostgreSQLServer().withExtensions("pg_trgm", "uuid-ossp").use { postgres ->
                postgres.start()

                assertConnection(postgres)
                val rs = performQuery(
                    postgres,
                    "SELECT extname FROM pg_extension WHERE extname IN ('pg_trgm','uuid-ossp') ORDER BY extname"
                )
                rs.getString("extname") shouldBeEqualTo "pg_trgm"
                rs.next()
                rs.getString("extname") shouldBeEqualTo "uuid-ossp"
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { PostgreSQLServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { PostgreSQLServer(tag = " ") }
    }
}
