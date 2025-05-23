package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MariaDBServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch mariadb server`() {
            MariaDBServer().use { mariadb ->
                mariadb.start()

                log.debug { "Connection URL: ${mariadb.jdbcUrl}" }
                assertConnection(mariadb)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch mariadb server with default port`() {
            MariaDBServer(useDefaultPort = true).use { mariadb ->
                mariadb.start()
                mariadb.port shouldBeEqualTo MariaDBServer.PORT

                log.debug { "Connection URL: ${mariadb.jdbcUrl}" }
                assertConnection(mariadb)
            }
        }
    }
}
