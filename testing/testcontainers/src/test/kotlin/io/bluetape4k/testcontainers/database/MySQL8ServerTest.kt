package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MySQL8ServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch mysql 8 server`() {
            MySQL8Server().use { mysql ->
                mysql.start()
                assertConnection(mysql)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch mysql 8 server with default port`() {
            MySQL8Server(useDefaultPort = true).use { mysql ->
                mysql.start()
                mysql.port shouldBeEqualTo MySQL8Server.PORT
                assertConnection(mysql)
            }
        }
    }
}
